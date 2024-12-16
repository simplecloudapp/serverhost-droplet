package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime
import app.simplecloud.droplet.serverhost.runtime.host.ServerVersionLoader
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.template.TemplateProvider
import app.simplecloud.droplet.serverhost.runtime.util.JarMainClass
import app.simplecloud.droplet.serverhost.runtime.util.ProcessDirectory
import app.simplecloud.droplet.serverhost.runtime.util.ScreenCapabilities
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes
import app.simplecloud.droplet.serverhost.shared.hack.PortProcessHandle
import app.simplecloud.droplet.serverhost.shared.hack.ServerPinger
import app.simplecloud.serverhost.configurator.ConfiguratorExecutor
import build.buf.gen.simplecloud.controller.v1.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class ServerRunner(
    private val configurator: ConfiguratorExecutor,
    private val templateProvider: TemplateProvider,
    private val serverHost: ServerHost,
    private val args: ServerHostStartCommand,
    private val controllerStub: ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub,
) {

    private val copyTemplateMutex = Mutex()

    private val defaultOptions =
        listOf(
            "-Xms%MIN_MEMORY%M",
            "-Xmx%MAX_MEMORY%M",
            "-Dcom.mojang.eula.agree=true",
            "-cp",
            "${args.libsPath.absolutePathString()}${File.separator}*${File.pathSeparator}%SERVER_FILE%",
            "%MAIN_CLASS%"
        )
    private val defaultArguments = listOf("nogui")
    private val defaultExecutable: String = File(System.getProperty("java.home"), "bin/java").absolutePath
    private val screenExecutable: String = "screen"

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

    fun getServer(uniqueId: String): Server? {
        return serverToProcessHandle.keys.find { it.uniqueId == uniqueId }
    }

    private fun updateServerCache(uniqueId: String, updated: Server) {
        val key = serverToProcessHandle.keys.find { it.uniqueId == uniqueId }
        if (key == null) {
            logger.warn("Server ${updated.group}-${updated.numericalId} could not be updated in cache")
            return
        }
        val value = serverToProcessHandle[key]!!
        serverToProcessHandle.remove(key)
        serverToProcessHandle[updated] = value
    }

    private suspend fun updateServer(server: Server?): Server? {
        if (server == null) {
            return null
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
        try {
            val ping = ServerPinger.ping(address)
            if (handle == null) return null
            PortProcessHandle.removePreBind(server.port.toInt())
            val controllerServer = controllerStub.getServerById(getServerByIdRequest {
                this.serverId = server.uniqueId
            })

            val copiedServer = Server.fromDefinition(controllerServer.copy {
                this.serverState =
                    if (ping.description.text == "INGAME")
                        ServerState.INGAME
                    else if (server.state == ServerState.STARTING)
                        ServerState.AVAILABLE
                    else
                        server.state
                this.maxPlayers = ping.players.max.toLong()
                this.playerCount = ping.players.online.toLong()
                this.cloudProperties["motd"] = ping.description.text
            })
            return copiedServer
        } catch (e: Exception) {
            logger.warn("Failed to ping server ${server.group}-${server.numericalId} ${server.ip}:${server.port}: ${e.message}")
            val portBound = PortProcessHandle.isPortBound(server.port.toInt())
            if (!portBound) {
                stopServer(server)
                return null
            }
            return server
        }
    }

    private fun getServerDir(server: Server): File {
        return getServerDir(server, GroupRuntime.Config.load<GroupRuntime>("${server.group}.yml"))
    }

    private fun getServerDir(server: Server, runtimeConfig: GroupRuntime?): File {
        var basicUrl = runtimeConfig?.parentDir ?: "${server.group}/${server.group}-${server.numericalId}"

        if (!basicUrl.startsWith("/")) {
            basicUrl = "${args.runningServersPath}/$basicUrl"
        }

        return File(basicUrl)
    }

    fun getServerLogFile(server: Server): Path {
        return Paths.get(
            args.logsPath.absolutePathString(),
            "${server.group}-${server.numericalId}-${server.uniqueId}.log"
        )
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

        if (server.properties["configurator"] == null) {
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Group has no assigned configurator.")
            return false
        }

        val ctx = executeTemplate(getServerDir(server).toPath(), server, YamlActionTriggerTypes.START)
        var serverDir: File? = null
        if (ctx != null) {
            serverDir = getServerDir(ctx)
        }

        val builder = buildProcess(serverDir, server, runtimeConfig)

        if (!builder.directory().exists()) {
            builder.directory().mkdirs()
        }

        val process = builder.start()
        serverToProcessHandle[server] = process.toHandle()
        logger.info("Server ${server.uniqueId} of group ${server.group} now running on PID ${process.pid()}")
        return true
    }

    private fun executeTemplate(dir: Path, server: Server, on: YamlActionTriggerTypes): YamlActionContext? {
        val template = templateProvider.getLoadedTemplate(server.properties["template-id"] ?: "")
        if (template != null) {
            return templateProvider.execute(server, dir, template, on)
        } else {
            if (!server.properties.containsKey("template-id"))
                logger.error("Group ${server.group} has no template defined!")
            else
                logger.error("Template ${server.properties["template-id"] ?: ""} of group ${server.group} was not found!")
        }
        return null;
    }

    private fun getServerDir(ctx: YamlActionContext): File? {
        val dir = ctx.retrieve<String>("server-dir") ?: return null
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx) ?: return null
        return Paths.get(placeholders.parse(dir)).toFile()
    }

    suspend fun stopServer(server: Server): Boolean {
        logger.info("Stopping server ${server.uniqueId} of group ${server.group} (#${server.numericalId})")
        val stopped = stopServer(server.uniqueId, stopTries.getOrDefault(server.uniqueId, 0) >= maxGracefulTries)
        if (!stopped) return false

        copyTemplateMutex.withLock {
            executeTemplate(getServerDir(server).toPath(), server, YamlActionTriggerTypes.STOP)
        }

        logger.info("Server ${server.uniqueId} of group ${server.group} successfully stopped.")
        PortProcessHandle.removePreBind(server.port.toInt(), true)
        return true
    }

    private suspend fun stopServer(uniqueId: String, forcibly: Boolean = false): Boolean {
        val server = getServer(uniqueId)
        if (server == null) {
            logger.error("Could not find server $uniqueId")
            return false
        }

        val process = serverToProcessHandle[server]
        if (process == null) {
            logger.error("Could not find server process of server ${server.group}-${server.numericalId}")
            return false
        }

        val load = GroupRuntime.Config.load<GroupRuntime>("${server.group}.yml")

        load?.jvm?.screenStop.let {
            if (it != null) {
                val screenSessionName = "${server.group}-${server.numericalId}-${server.uniqueId.substring(0, 6)}"
                terminateScreenSession(screenSessionName)
            } else {
                if (!forcibly)
                    process.destroy()
                else
                    process.destroyForcibly()
            }
        }

        stopTries[uniqueId] = stopTries.getOrDefault(uniqueId, 0) + 1
        try {
            process.onExit().await()
            serverToProcessHandle.remove(server)
            stopTries.remove(uniqueId)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun getProcess(uniqueId: String): ProcessHandle? {
        return serverToProcessHandle.getOrDefault(
            serverToProcessHandle.keys.firstOrNull { it.uniqueId == uniqueId },
            null
        )
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
            executeTemplate(getServerDir(server).toPath(), server, YamlActionTriggerTypes.STOP)
            FileUtils.deleteDirectory(getServerDir(server))
            PortProcessHandle.removePreBind(server.port.toInt(), true)
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

        val parent = handle.parent().orElse(null)
        if (parent == null || parent.info().command().orElse(null)
                ?.startsWith(registeredServer.properties.getOrDefault("executable", defaultExecutable)) == false
        ) {
            return Optional.empty()
        }

        return getRealProcessParent(registeredServer, parent)
    }

    private fun terminateScreenSession(screenSessionId: String) {
        try {
            val command = "screen -S $screenSessionId -X quit"
            val process = ProcessBuilder(command.split(" "))
                .redirectErrorStream(true)
                .start()

            process.waitFor()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun checkAcceptance(runtimeConfig: GroupRuntime?): Boolean {
        return runtimeConfig == null || runtimeConfig.ignore != true
    }

    private fun buildProcess(serverDir: File?, server: Server, runtimeConfig: GroupRuntime?): ProcessBuilder {
        val jvmArgs: JvmArguments = runtimeConfig?.jvm ?: crateDefaultJvmArguments()
        val command = mutableListOf<String>()
        command.add(jvmArgs.executable ?: defaultExecutable)
        val serverJar = ServerVersionLoader.getAndDownloadServerJar(server.properties["server-url"]!!).toPath()

        val logFile = getServerLogFile(server)
        if (!logFile.parent.exists()) {
            FileUtils.createParentDirectories(logFile.toFile())
        }

        val placeholders = mutableMapOf(
            "%MIN_MEMORY%" to server.minMemory.toString(),
            "%MAX_MEMORY%" to server.maxMemory.toString(),
            "%SCREEN_NAME%" to "${server.group}-${server.numericalId}-${server.uniqueId.substring(0, 6)}",
            "%NUMERICAL_ID%" to server.numericalId.toString(),
            "%GROUP%" to server.group,
            "%UNIQUE_ID%" to server.uniqueId,
            "%MAIN_CLASS%" to JarMainClass.find(serverJar),
            "%SERVER_FILE%" to serverJar.absolutePathString(),
            "%LOG_FILE%" to logFile.absolutePathString(),
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

        jvmArgs.screenStop = placeholders["%SCREEN_NAME%"]

        //TODO exist check before save
        GroupRuntime.Config.save(server.group, GroupRuntime(jvmArgs, null, null))

        val builder = ProcessBuilder()
            .command(command)
            .directory(serverDir ?: getServerDir(server, runtimeConfig))
        builder.environment()["HOST_IP"] = serverHost.host
        builder.environment()["HOST_PORT"] = serverHost.port.toString()
        builder.environment()["CONTROLLER_HOST"] = this.args.grpcHost
        builder.environment()["CONTROLLER_PORT"] = this.args.grpcPort.toString()
        builder.environment()["CONTROLLER_SECRET"] = this.args.authSecret
        builder.environment()["CONTROLLER_PUBSUB_HOST"] = this.args.pubSubGrpcHost
        builder.environment()["CONTROLLER_PUBSUB_PORT"] = this.args.pubSubGrpcPort.toString()
        builder.environment().putAll(server.toEnv())
        if (jvmArgs.executable?.lowercase() != "screen")
            builder.redirectOutput(logFile.toFile())
        return builder
    }

    private fun crateDefaultJvmArguments(): JvmArguments {
        val useScreen = ScreenCapabilities.isScreenAvailable()
        val capabilities = if (useScreen) ScreenCapabilities.getScreenCapabilities() else ScreenCapabilities()

        return when {
            useScreen -> {
                val screenOpts = mutableListOf("-dmS", "%SCREEN_NAME%", defaultExecutable)

                if (capabilities.hasLogging && capabilities.hasLogFile) {
                    screenOpts.addAll(0, listOf("-L", "-Logfile", "%LOG_FILE%"))
                } else if (capabilities.hasLogging) {
                    screenOpts.add(0, "-L")
                }

                JvmArguments(
                    screenExecutable,
                    screenOpts + defaultOptions,
                    defaultArguments,
                    null
                )
            }

            else -> {
                JvmArguments(
                    defaultExecutable,
                    defaultOptions,
                    defaultArguments,
                    null
                )
            }
        }
    }

    fun startServerStateChecker(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                serverToProcessHandle.keys.toList().forEach {

                    var delete = false
                    var server = it
                    try {
                        val updated = updateServer(it)
                        if (updated == null) delete = true
                        else {
                            server = updated
                            updateServerCache(updated.uniqueId, updated)
                        }
                        controllerStub.updateServer(
                            UpdateServerRequest.newBuilder()
                                .setServer(server.toDefinition())
                                .setDeleted(delete).build()
                        )
                    } catch (e: Exception) {
                        logger.error("An error occurred whilst updating the server:", e)
                    }
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