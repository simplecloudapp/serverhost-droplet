package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime
import app.simplecloud.droplet.serverhost.runtime.configurator.ServerConfiguratorExecutor
import app.simplecloud.droplet.serverhost.runtime.hack.PortProcessHandle
import app.simplecloud.droplet.serverhost.runtime.hack.ServerPinger
import app.simplecloud.droplet.serverhost.runtime.host.ServerVersionLoader
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.template.TemplateActionType
import app.simplecloud.droplet.serverhost.runtime.template.TemplateCopier
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpc
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerUpdateRequest
import build.buf.gen.simplecloud.controller.v1.copy
import kotlinx.coroutines.*
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class ServerRunner(
    private val serverVersionLoader: ServerVersionLoader,
    private val configurator: ServerConfiguratorExecutor,
    private val templateCopier: TemplateCopier,
    private val serverHost: ServerHost,
    private val args: ServerHostStartCommand
) {

    private val logger = LogManager.getLogger(ServerHostRuntime::class.java)

    private val running = mutableMapOf<Server, ProcessHandle>()

    private fun containsServer(server: Server): Boolean {
        return running.any { it.key.uniqueId == server.uniqueId }
    }

    private fun containsServer(uniqueId: String): Boolean {
        return running.any { it.key.uniqueId == uniqueId }
    }

    private fun getServer(uniqueId: String): Server? {
        return running.keys.find { it.uniqueId == uniqueId }
    }

    private val channel = ServerHostRuntime.createControllerChannel()
    private val stub = ControllerServerServiceGrpc.newFutureStub(channel)

    private fun updateServer(it: Server): CompletableFuture<Server?> {
        val address = InetSocketAddress(it.ip, it.port.toInt())
        return ServerPinger.ping(address).thenApply { response ->
            val server = Server.fromDefinition(it.toDefinition().copy {
                this.state =
                    if (response.description.text == "INGAME") ServerState.INGAME else if (it.state == ServerState.STARTING) ServerState.AVAILABLE else it.state
                this.maxPlayers = response.players.max.toLong()
                this.playerCount = response.players.online.toLong()
                this.properties.put("motd", response.description.text)
            })
            return@thenApply server
        }.exceptionally { _ ->
            if (LocalDateTime.now().isAfter(
                    it.createdAt.plusSeconds(
                        it.properties.getOrDefault("max-startup-seconds", "120").toLong()
                    )
                )
            ) {
                stopServer(it)
                return@exceptionally null
            } else {
                return@exceptionally it
            }
        }
    }

    val DEFAULT_OPTIONS = listOf("-Xms%MIN_MEMORY%M", "-Xmx%MAX_MEMORY%M", "-Dcom.mojang.eula.agree=true", "-jar")
    val DEFAULT_ARGUMENTS = listOf("nogui")
    val DEFAULT_EXECUTABLE: String = File(System.getProperty("java.home"), "bin/java").absolutePath

    fun getServerDir(server: Server): File {
        return getServerDir(server, GroupRuntime.Config.load<GroupRuntime>("${server.group}.yml"))
    }

    private fun getServerDir(server: Server, runtimeConfig: GroupRuntime?): File {
        var basicUrl =
            if (runtimeConfig?.parentDir != null) runtimeConfig.parentDir else "${server.group}/${server.group}-${server.numericalId}"
        if (!basicUrl.startsWith("/")) basicUrl = "${args.runningServersPath}/$basicUrl"
        return File(basicUrl)
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
        serverVersionLoader.download(server)
        val builder = buildProcess(server, runtimeConfig)
        if (!builder.directory().exists()) builder.directory().mkdirs()
        templateCopier.copy(server, this, TemplateActionType.DEFAULT)
        templateCopier.copy(server, this, TemplateActionType.RANDOM)
        if (!configurator.configurate(server, this)) {
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Failed to configure server.")
            Files.delete(getServerDir(server).toPath())
            return false
        }
        val process = builder.start()
        running[server] = process.toHandle()
        logger.info("Server ${server.uniqueId} of group ${server.group} now running on PID ${process.pid()}")
        return true
    }

    fun stopServer(server: Server): CompletableFuture<Boolean> {
        logger.info("Stopping server ${server.uniqueId} of group ${server.group} (#${server.numericalId})")
        return stopServer(server.uniqueId).thenApply {
            if (!it) return@thenApply false
            templateCopier.copy(server, this, TemplateActionType.SHUTDOWN)
            FileUtils.deleteDirectory(getServerDir(server))
            logger.info("Server ${server.uniqueId} of group ${server.group} successfully stopped.")
            return@thenApply true
        }
    }

    private fun stopServer(uniqueId: String): CompletableFuture<Boolean> {
        val server = getServer(uniqueId) ?: return CompletableFuture.completedFuture(false)
        val process = running[server] ?: return CompletableFuture.completedFuture(false)
        process.destroy()
        return process.onExit().thenApply {
            running.remove(server)
            return@thenApply true
        }
    }

    fun reattachServer(server: Server): Boolean {
        if (containsServer(server.uniqueId)) {
            logger.error("Server ${server.uniqueId} of group ${server.group} is already running.")
            return true
        }
        val handle = PortProcessHandle.of(server.port.toInt()).orElse(null)
        if (handle == null) {
            logger.error("Server ${server.uniqueId} of group ${server.group} not found running on port ${server.port}. Is it down?")
            return false
        }
        running[server] = handle
        logger.info("Server ${server.uniqueId} of group ${server.group} successfully reattached on PID ${handle.pid()}")
        return true
    }

    private fun checkAcceptance(runtimeConfig: GroupRuntime?): Boolean {
        return runtimeConfig == null || runtimeConfig.ignore != true
    }

    private fun buildProcess(server: Server, runtimeConfig: GroupRuntime?): ProcessBuilder {
        val args: JvmArguments = if (runtimeConfig?.jvm != null) runtimeConfig.jvm else JvmArguments(
            DEFAULT_EXECUTABLE,
            DEFAULT_OPTIONS,
            DEFAULT_ARGUMENTS
        )
        GroupRuntime.Config.save(server.group, GroupRuntime(args, null, null))
        val command = mutableListOf<String>()
        command.add(args.executable ?: DEFAULT_EXECUTABLE)
        val placeholders = mutableMapOf(
            "%MIN_MEMORY%" to server.minMemory.toString(),
            "%MAX_MEMORY%" to server.maxMemory.toString(),
        )
        placeholders.putAll(server.properties.map {
            "%${it.key.uppercase().replace("-", "_")}%" to it.value
        })

        if (!args.options.isNullOrEmpty()) command.addAllWithPlaceholders(args.options, placeholders)
        command.add(serverVersionLoader.getServerJar(server).absolutePath)
        if (!args.arguments.isNullOrEmpty()) command.addAllWithPlaceholders(args.arguments, placeholders)
        val builder = ProcessBuilder()
            .command(command)
            .directory(getServerDir(server, runtimeConfig))
        builder.environment()["HOST_IP"] = serverHost.host
        builder.environment()["HOST_PORT"] = serverHost.port.toString()
        builder.environment()["CONTROLLER_IP"] = System.getenv("CONTROLLER_IP") ?: "127.0.0.1"
        builder.environment()["CONTROLLER_PORT"] = System.getenv("CONTROLLER_PORT") ?: "5816"
        return builder
    }

    fun startServerStateChecker(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                running.keys.toList().forEach {
                    var delete = false
                    var server = it
                    updateServer(it).thenApply { then ->
                        if (then == null) delete = true
                        else server = then
                    }.exceptionally {
                        delete = true
                    }.get()

                    stub.updateServer(
                        ServerUpdateRequest.newBuilder()
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