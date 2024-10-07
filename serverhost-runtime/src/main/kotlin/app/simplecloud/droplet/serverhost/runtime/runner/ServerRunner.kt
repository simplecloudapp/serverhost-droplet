package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime
import app.simplecloud.droplet.serverhost.runtime.configurator.ServerConfiguratorExecutor
import app.simplecloud.droplet.serverhost.runtime.hack.JarMainClass
import app.simplecloud.droplet.serverhost.runtime.hack.ProcessDirectory
import app.simplecloud.droplet.serverhost.runtime.host.ServerVersionLoader
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.template.TemplateActionType
import app.simplecloud.droplet.serverhost.runtime.template.TemplateCopier
import app.simplecloud.droplet.serverhost.shared.grpc.ServerHostGrpc
import app.simplecloud.droplet.serverhost.shared.hack.OS
import app.simplecloud.droplet.serverhost.shared.hack.PortProcessHandle
import app.simplecloud.droplet.serverhost.shared.hack.ServerPinger
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpc
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.UpdateServerRequest
import build.buf.gen.simplecloud.controller.v1.copy
import kotlinx.coroutines.*
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.absolutePathString

class ServerRunner(
    private val configurator: ServerConfiguratorExecutor,
    private val templateCopier: TemplateCopier,
    private val serverHost: ServerHost,
    private val args: ServerHostStartCommand,
    authCallCredentials: AuthCallCredentials,
) {

    private val defaultOptions =
        listOf("-Xms%MIN_MEMORY%M", "-Xmx%MAX_MEMORY%M", "-Dcom.mojang.eula.agree=true", "-cp", "${args.libsPath.absolutePathString()}${File.separator}*${File.pathSeparator}%SERVER_FILE%", "%MAIN_CLASS%")
    private val defaultArguments = listOf("nogui")
    private val defaultExecutable: String = File(System.getProperty("java.home"), "bin/java").absolutePath
    private val screenExecutable: String = "screen"
    private val screenOptions =
        mutableListOf("-dmS", "%SCREEN_NAME%", defaultExecutable, *defaultOptions.toTypedArray())

    private val channel = ServerHostGrpc.createControllerChannel(args.grpcHost, args.grpcPort)
    private val stub = ControllerServerServiceGrpc.newFutureStub(channel)
        .withCallCredentials(authCallCredentials)

    private val stopTries = mutableMapOf<String, Int>()
    private val maxGracefulTries = 3

    private val logger = LogManager.getLogger(ServerHostRuntime::class.java)

    private val serverToProcessHandle = mutableMapOf<Server, ProcessHandle>()

    private fun containsServer(server: Server): Boolean {
        return serverToProcessHandle.any { it.key.uniqueId == server.uniqueId }
    }

    private fun containsServer(uniqueId: String): Boolean {
        return serverToProcessHandle.any { it.key.uniqueId == uniqueId }
    }

    private fun getServer(uniqueId: String): Server? {
        return serverToProcessHandle.keys.find { it.uniqueId == uniqueId }
    }

    private fun updateServerCache(uniqueId: String, updated: Server) {
        val key = serverToProcessHandle.keys.find { it.uniqueId == uniqueId }
        val value = serverToProcessHandle[key]!!
        serverToProcessHandle.remove(key)
        serverToProcessHandle[updated] = value
    }

    private fun updateServer(server: Server?): CompletableFuture<Server?> {
        if (server == null) {
            return CompletableFuture.completedFuture(null)
        }

        // Retrieving this before the ping makes it possible to stop servers way sooner (port is registered in system nearly instantly, it takes longer for the
        // server to respond to pings though)
        val handle = PortProcessHandle.of(server.port.toInt()).orElse(null)?.let {
            val realProcess = getRealProcessParent(server, it).orElse(null)
            if (realProcess != null && serverToProcessHandle[server] != realProcess) {
                logger.info("Found updated process handle with PID ${realProcess.pid()} for ${server.group}-${server.numericalId}")
                serverToProcessHandle[server] = realProcess
            }
            return@let serverToProcessHandle[server]
        }

        val address = InetSocketAddress(server.ip, server.port.toInt())
        return ServerPinger.ping(address).thenApply { response ->
            if (handle == null) return@thenApply null
            PortProcessHandle.removePreBind(server.port.toInt())
            val copiedServer = Server.fromDefinition(server.toDefinition().copy {
                this.serverState =
                    if (response.description.text == "INGAME")
                        ServerState.INGAME
                    else if (server.state == ServerState.STARTING)
                        ServerState.AVAILABLE
                    else
                        server.state
                this.maxPlayers = response.players.max.toLong()
                this.playerCount = response.players.online.toLong()
                this.cloudProperties["motd"] = response.description.text
            })
            return@thenApply copiedServer
        }.exceptionally { _ ->
            val portBound = PortProcessHandle.isPortBound(server.port.toInt())
            if (!portBound) {
                stopServer(server)
                return@exceptionally null
            }

            return@exceptionally server
        }
    }

    fun getServerDir(server: Server): File {
        return getServerDir(server, GroupRuntime.Config.load<GroupRuntime>("${server.group}.yml"))
    }

    private fun getServerDir(server: Server, runtimeConfig: GroupRuntime?): File {
        var basicUrl = runtimeConfig?.parentDir ?: "${server.group}/${server.group}-${server.numericalId}"

        if (!basicUrl.startsWith("/")) {
            basicUrl = "${args.runningServersPath}/$basicUrl"
        }

        return File(basicUrl)
    }

    private fun isServerDir(server: Server, path: Path): Boolean {
        return path == getServerDir(server).toPath()
    }

    fun startServer(server: Server): Boolean {
        logger.info("Starting server ${server.uniqueId} of group ${server.group} (#${server.numericalId})")

        if (containsServer(server)) {
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Server with this id already exists.")
            return false
        }
        val runtimeConfig = GroupRuntime.Config.load<GroupRuntime>("${server.group}.yml")
        if (!checkAcceptance(runtimeConfig)) {
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Group not supported by this ServerHost.")
            return false
        }

        val builder = buildProcess(server, runtimeConfig)

        if (!builder.directory().exists()) {
            builder.directory().mkdirs()
        }
        templateCopier.copy(server, this, TemplateActionType.DEFAULT)
        templateCopier.copy(server, this, TemplateActionType.RANDOM)
        val configuratorSuccess = configurator.configurate(server, this)
        if (!configuratorSuccess) {
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Failed to configure server.")
            FileUtils.deleteDirectory(getServerDir(server))
            return false
        }
        val process = builder.start()
        serverToProcessHandle[server] = process.toHandle()
        logger.info("Server ${server.uniqueId} of group ${server.group} now running on PID ${process.pid()}")
        return true
    }

    fun stopServer(server: Server): CompletableFuture<Boolean> {
        logger.info("Stopping server ${server.uniqueId} of group ${server.group} (#${server.numericalId})")
        return stopServer(server.uniqueId, stopTries.getOrDefault(server.uniqueId, 0) >= maxGracefulTries).thenApply {
            if (!it) return@thenApply false
            templateCopier.copy(server, this, TemplateActionType.SHUTDOWN)
            FileUtils.deleteDirectory(getServerDir(server))
            logger.info("Server ${server.uniqueId} of group ${server.group} successfully stopped.")
            PortProcessHandle.removePreBind(server.port.toInt())
            return@thenApply true
        }
    }

    private fun stopServer(uniqueId: String, forcibly: Boolean = false): CompletableFuture<Boolean> {
        val server = getServer(uniqueId) ?: return CompletableFuture.completedFuture(false)
        val process = serverToProcessHandle[server] ?: return CompletableFuture.completedFuture(false)
        if (!forcibly)
            process.destroy()
        else
            process.destroyForcibly()
        stopTries[uniqueId] = stopTries.getOrDefault(uniqueId, 0) + 1
        return process.onExit().thenApply {
            serverToProcessHandle.remove(server)
            stopTries.remove(uniqueId)
            return@thenApply true
        }
    }

    fun reattachServer(server: Server): Boolean {
        if (containsServer(server.uniqueId)) {
            logger.error("Server ${server.uniqueId} of group ${server.group} is already running.")
            return true
        }
        val handle = PortProcessHandle.of(server.port.toInt()).orElse(null)
            ?.let { getRealProcessParent(server, it).orElse(null) }
        if (handle == null) {
            logger.error("Server ${server.uniqueId} of group ${server.group} not found running on port ${server.port}. Is it down?")
            templateCopier.copy(server, this, TemplateActionType.SHUTDOWN)
            FileUtils.deleteDirectory(getServerDir(server))
            PortProcessHandle.removePreBind(server.port.toInt())
            return false
        }
        serverToProcessHandle[server] = handle
        logger.info("Server ${server.uniqueId} of group ${server.group} successfully reattached on PID ${handle.pid()}")
        return true
    }

    /**
     * This method is intended solve a bug where servers can not stop correctly.
     * This works by searching for the highest parent process of a [ProcessHandle]
     * which is a process that has a server dir as execution environment.
     */
    private fun getRealProcessParent(registeredServer: Server, handle: ProcessHandle): Optional<ProcessHandle> {
        val path = ProcessDirectory.of(handle).orElse(getServerDir(registeredServer).toPath())
        if (isServerDir(registeredServer, path)) {
            return Optional.of(handle)
        }
        return handle.parent().orElse(null)?.let { getRealProcessParent(registeredServer, it) } ?: Optional.empty()
    }

    private fun checkAcceptance(runtimeConfig: GroupRuntime?): Boolean {
        return runtimeConfig == null || runtimeConfig.ignore != true
    }

    private fun buildProcess(server: Server, runtimeConfig: GroupRuntime?): ProcessBuilder {
        val os = OS.get()
        val jvmArgs: JvmArguments = runtimeConfig?.jvm ?: crateDefaultJvmArguments(os)
        //TODO exist check before save
        GroupRuntime.Config.save(server.group, GroupRuntime(jvmArgs, null, null))
        val command = mutableListOf<String>()
        command.add(jvmArgs.executable ?: defaultExecutable)
        val serverJar = ServerVersionLoader.getAndDownloadServerJar(server.properties["server-url"]!!).toPath()
        val placeholders = mutableMapOf(
            "%MIN_MEMORY%" to server.minMemory.toString(),
            "%MAX_MEMORY%" to server.maxMemory.toString(),
            "%SCREEN_NAME%" to "${server.group}-${server.numericalId}-${server.uniqueId.substring(0, 6)}",
            "%NUMERICAL_ID%" to server.numericalId.toString(),
            "%GROUP%" to server.group,
            "%UNIQUE_ID%" to server.uniqueId,
            "%MAIN_CLASS%" to JarMainClass.find(serverJar),
            "%SERVER_FILE%" to serverJar.absolutePathString(),
        )
        placeholders.putAll(server.properties.map {
            "%${it.key.uppercase().replace("-", "_")}%" to it.value
        })

        if (!jvmArgs.options.isNullOrEmpty()) {
            command.addAllWithPlaceholders(jvmArgs.options, placeholders)
        }
        if (!jvmArgs.arguments.isNullOrEmpty()) {
            command.addAllWithPlaceholders(jvmArgs.arguments, placeholders)
        }
        val builder = ProcessBuilder()
            .command(command)
            .directory(getServerDir(server, runtimeConfig))
        builder.environment()["HOST_IP"] = serverHost.host
        builder.environment()["HOST_PORT"] = serverHost.port.toString()
        builder.environment()["CONTROLLER_HOST"] = this.args.grpcHost
        builder.environment()["CONTROLLER_PORT"] = this.args.grpcPort.toString()
        builder.environment()["CONTROLLER_SECRET"] = this.args.authSecret
        builder.environment()["CONTROLLER_PUBSUB_HOST"] = this.args.pubSubGrpcHost
        builder.environment()["CONTROLLER_PUBSUB_PORT"] = this.args.pubSubGrpcPort.toString()
        builder.environment().putAll(server.toEnv())
        return builder
    }

    private fun crateDefaultJvmArguments(os: OS?): JvmArguments {
        return JvmArguments(
            if (os == OS.UNIX) screenExecutable else defaultExecutable,
            if (os == OS.UNIX) screenOptions else defaultOptions,
            defaultArguments
        )
    }


    fun startServerStateChecker(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                serverToProcessHandle.keys.toList().forEach {
                    var delete = false
                    var server = it
                    updateServer(it).thenApply { then ->
                        if (then == null) delete = true
                        else {
                            server = then
                            updateServerCache(then.uniqueId, then)
                        }
                    }.exceptionally { error ->
                        logger.error("An error occurred whilst updating the server:", error)
                        delete = true
                    }.get()

                    stub.updateServer(
                        UpdateServerRequest.newBuilder()
                            .setServer(server.toDefinition())
                            .setDeleted(delete).build()
                    )
                }
                delay(5000L)
            }
        }
    }

    private fun MutableList<String>.addAllWithPlaceholders(commands: List<String>, placeholders: Map<String, String>) {
        addAll(commands.map {
            var returned = it
            placeholders.keys.map { placeholder ->
                returned = returned.replace(placeholder, placeholders.getOrDefault(placeholder, ""))
            }
            return@map returned
        })
    }

}