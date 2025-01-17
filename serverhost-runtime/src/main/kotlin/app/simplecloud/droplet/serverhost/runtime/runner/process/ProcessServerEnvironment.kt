package app.simplecloud.droplet.serverhost.runtime.runner.process

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfig
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfigRepository
import app.simplecloud.droplet.serverhost.runtime.host.ServerVersionLoader
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntime
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntimeDirectory
import app.simplecloud.droplet.serverhost.runtime.runner.MetricsTracker
import app.simplecloud.droplet.serverhost.runtime.runner.ServerEnvironment
import app.simplecloud.droplet.serverhost.runtime.template.TemplateProvider
import app.simplecloud.droplet.serverhost.runtime.util.JarMainClass
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes
import app.simplecloud.droplet.serverhost.shared.hack.PortProcessHandle
import app.simplecloud.droplet.serverhost.shared.hack.ServerPinger
import app.simplecloud.droplet.serverhost.shared.logs.DefaultLogStreamer
import app.simplecloud.droplet.serverhost.shared.logs.ScreenCommandExecutor
import app.simplecloud.droplet.serverhost.shared.logs.ScreenConfigurer
import app.simplecloud.droplet.serverhost.shared.process.ProcessFinder
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpcKt
import build.buf.gen.simplecloud.controller.v1.ServerHostStreamServerLogsResponse
import build.buf.gen.simplecloud.controller.v1.copy
import build.buf.gen.simplecloud.controller.v1.updateServerRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class ProcessServerEnvironment(
    private val templateProvider: TemplateProvider,
    private val serverHost: ServerHost,
    private val args: ServerHostStartCommand,
    private val controllerStub: ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub,
    private val metricsTracker: MetricsTracker,
    private val environmentsRepository: EnvironmentConfigRepository,
    override val runtimeRepository: GroupRuntimeDirectory,
) : ServerEnvironment(runtimeRepository, environmentsRepository) {

    private val copyTemplateMutex = Mutex()

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

    override fun getServer(uniqueId: String): Server? {
        return serverToProcessHandle.keys.find { it.uniqueId == uniqueId }
    }

    override fun getServerCache(): MutableMap<Server, *> {
        return serverToProcessHandle
    }

    override suspend fun updateServer(server: Server): Server? {
        // Retrieving this before the ping makes it possible to stop servers way sooner (port is registered in system nearly instantly, it takes longer for the
        // server to respond to pings though)
        val executable = getEnvironment(server)
        val handle = PortProcessHandle.of(server.port.toInt()).orElse(null)?.let {
            val realProcess = executable?.getRealExecutable()?.let { exe ->
                ProcessFinder.findHighestProcessWorkingDir(
                    getServerDir(server).toPath(), it,
                    exe
                ).orElse(null)
            }
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
            val copiedServer = updateServer(server, ping, controllerStub)
            trackMetrics(copiedServer, handle, executable?.getExecutable())
            return copiedServer
        } catch (e: Exception) {
            logger.warn("Failed to ping server ${server.group}-${server.numericalId} ${server.ip}:${server.port}: ${e.message}")
            val portBound = PortProcessHandle.isPortBound(server.port.toInt())
            if (!portBound) {
                return null
            }
            return server
        }
    }

    private fun trackMetrics(
        copiedServer: Server,
        handle: ProcessHandle,
        executable: String?
    ) {
        try {
            metricsTracker.trackPlayers(copiedServer)
        } catch (e: Exception) {
            logger.warn("Failed to track player metrics: ${e.message}")
        }

        try {
            metricsTracker.trackRamAndCpu(
                copiedServer,
                ProcessFinder.findHighestExecutableProcess(handle, executable ?: "unknown").get()
            )
        } catch (e: Exception) {
            logger.warn("Failed to track ram and cpu metrics: ${e.message}")
        }
    }

    private fun getServerDir(server: Server): File {
        return if (server.properties.containsKey("server-dir")) Path.of(server.properties["server-dir"]!!)
            .toFile() else getServerDir(server, runtimeRepository.get(server.group))
    }

    private fun getServerDir(server: Server, runtimeConfig: GroupRuntime?): File {
        var basicUrl = runtimeConfig?.parentDir ?: "${server.group}/${server.group}-${server.numericalId}"

        if (!basicUrl.startsWith("/")) {
            basicUrl = "${args.runningServersPath}/$basicUrl"
        }

        return File(basicUrl)
    }

    private fun getServerLogFile(server: Server): Path {
        return Paths.get(
            args.logsPath.absolutePathString(),
            "${server.group}-${server.numericalId}-${server.uniqueId}.log"
        )
    }

    override suspend fun startServer(server: Server): Boolean {
        logger.info("Starting server ${server.uniqueId} of group ${server.group} (#${server.numericalId})")

        if (containsServer(server)) {
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Server with this id already exists.")
            return false
        }
        val runtimeConfig = runtimeRepository.get(server.group)
        if (!checkAcceptance(runtimeConfig)) {
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Group not supported by this ServerHost.")
            return false
        }

        if (server.properties["configurator"] == null) {
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Group has no assigned configurator.")
            return false
        }
        var ctx: YamlActionContext?
        copyTemplateMutex.withLock {
            ctx = executeTemplate(getServerDir(server).toPath(), server, YamlActionTriggerTypes.START, templateProvider)
        }

        var serverDir: File? = null
        if (ctx != null) {
            serverDir = getServerDir(ctx!!)
        }
        try {
            val builder = buildProcess(serverDir, server, runtimeConfig)

            if (!builder.directory().exists()) {
                builder.directory().mkdirs()
            }
            val process = withContext(Dispatchers.IO) {
                builder.start()
            }
            val updatedServer = Server.fromDefinition(server.toDefinition().copy {
                cloudProperties["server-dir"] = serverDir?.absolutePath ?: getServerDir(server).absolutePath
            })
            CoroutineScope(Dispatchers.IO).launch {
                controllerStub.updateServer(updateServerRequest {
                    this.server = updatedServer.toDefinition()
                    this.deleted = false
                })
            }
            serverToProcessHandle[updatedServer] = process.toHandle()
            logger.info("Server ${server.uniqueId} of group ${server.group} now running on PID ${process.pid()}")
            return true
        } catch (e: Exception) {
            logger.error("Can not build process", e)
            return false
        }

    }

    private fun getServerDir(ctx: YamlActionContext): File? {
        val dir = ctx.retrieve<String>("server-dir") ?: return null
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx) ?: return null
        return Paths.get(placeholders.parse(dir)).toFile()
    }

    override suspend fun stopServer(server: Server): Boolean {
        logger.info("Stopping server ${server.uniqueId} of group ${server.group} (#${server.numericalId})")
        val stopped = stopServer(server.uniqueId, stopTries.getOrDefault(server.uniqueId, 0) >= maxGracefulTries)
        if (!stopped) return false

        copyTemplateMutex.withLock {
            executeTemplate(getServerDir(server).toPath(), server, YamlActionTriggerTypes.STOP, templateProvider)
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

        val env = getEnvironment(server)
        if (env != null && env.useScreenStop) {
            terminateScreenSession(process.pid())
        } else {
            if (!forcibly) {
                process.destroy()
            } else {
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

    private fun getProcess(uniqueId: String): ProcessHandle? {
        return serverToProcessHandle.getOrDefault(
            serverToProcessHandle.keys.firstOrNull { it.uniqueId == uniqueId },
            null
        )
    }


    override fun reattachServer(server: Server): Boolean {
        if (containsServer(server.uniqueId)) {
            logger.error("Server ${server.uniqueId} of group ${server.group} is already running.")
            return true
        }
        val executable = getEnvironment(server)?.getRealExecutable()
        val handle = PortProcessHandle.of(server.port.toInt()).orElse(null)
            ?.let {
                executable?.let { exe ->
                    ProcessFinder.findHighestProcessWorkingDir(
                        getServerDir(server).toPath(),
                        it,
                        exe
                    ).orElse(null)
                }
            }

        if (handle == null) {
            logger.error("Server ${server.uniqueId} of group ${server.group} not found running on port ${server.port}. Is it down?")
            executeTemplate(getServerDir(server).toPath(), server, YamlActionTriggerTypes.STOP, templateProvider)
            PortProcessHandle.removePreBind(server.port.toInt(), true)
            return false
        }

        serverToProcessHandle[server] = handle
        logger.info("Server ${server.uniqueId} of group ${server.group} successfully reattached on PID ${handle.pid()}")
        return true
    }

    override fun executeCommand(server: Server, command: String): Boolean {
        if (getEnvironment(server)?.isScreen != true) return false
        val process = getProcess(server.uniqueId)
            ?: return false

        val streamer = ScreenCommandExecutor(process.pid())
        if (!streamer.isScreen()) return false

        streamer.sendCommand(command)
        return true
    }

    override fun streamLogs(server: Server): Flow<ServerHostStreamServerLogsResponse> {
        var configurer: ScreenConfigurer? = null
        try {
            val process = getProcess(server.uniqueId)
            if (process != null) {
                configurer = ScreenConfigurer(process.pid())
                configurer.setLogsFlush(0)
                logger.warn("Screen streaming for server ${server.group}-${server.numericalId} (${server.uniqueId}) not available, log stream will be slower.")
            }
            val fileStreamer = DefaultLogStreamer(getServerLogFile(server))
            return fileStreamer.readScreenLogs().onCompletion { configurer?.setLogsFlush(10) }
        } catch (e: Exception) {
            configurer?.setLogsFlush(10)
            logger.error("Failed to stream server logs", e)
            throw e
        }
    }

    override fun appliesFor(env: EnvironmentConfig): Boolean {
        return env.enabled && env.start?.command != null
    }

    private fun terminateScreenSession(pid: Long) {
        try {
            logger.info("Terminating screen $pid")
            val process = ProcessBuilder("screen", "-S", pid.toString(), "-X", "quit")
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
        val envBuilder = ProcessEnvironmentBuilder(serverHost, args)
        var runtime = runtimeConfig
        if (runtimeConfig == null) {
            runtime = envBuilder.buildRuntime(server, runtimeRepository)
        }
        val env = environmentsRepository.get(runtime)
        if (env?.enabled != true) {
            throw IllegalArgumentException("Group ${server.group} is not using an enabled environment")
        }
        if (env.start?.command == null) {
            throw IllegalArgumentException("Group ${server.group} is not using an command start environment")
        }
        val serverJar = ServerVersionLoader.getAndDownloadServerJar(server.properties["server-url"]!!).toPath()
        val logFile = getServerLogFile(server)
        if (!logFile.parent.exists()) {
            FileUtils.createParentDirectories(logFile.toFile())
        }
        val placeholders = envBuilder.createRuntimePlaceholders(server).toMutableMap()
        placeholders.putAll(
            mapOf(
                "%MAIN_CLASS%" to JarMainClass.find(serverJar),
                "%SERVER_FILE%" to serverJar.absolutePathString(),
                "%LOG_FILE%" to logFile.absolutePathString(),
            )
        )
        val command = mutableListOf<String>()
        command.addAll(env.start.command)
        envBuilder.addAllWithPlaceholders(command, placeholders)
        val builder = ProcessBuilder()
            .command(command)
            .directory(serverDir ?: getServerDir(server, runtimeConfig))
        builder.environment().putAll(envBuilder.buildEnv(server))
        if (!env.isScreen) {
            builder.redirectOutput(logFile.toFile())
        }
        return builder
    }

    override fun getServers(): List<Server> {
        return serverToProcessHandle.keys.toList()
    }
}