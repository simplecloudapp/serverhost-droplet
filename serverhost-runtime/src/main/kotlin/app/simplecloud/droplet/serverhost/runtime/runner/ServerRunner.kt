package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime
import app.simplecloud.droplet.serverhost.runtime.configurator.ServerConfiguratorExecutor
import app.simplecloud.droplet.serverhost.runtime.hack.PortProcessHandle
import app.simplecloud.droplet.serverhost.runtime.host.ServerVersionLoader
import app.simplecloud.droplet.serverhost.runtime.template.TemplateActionType
import app.simplecloud.droplet.serverhost.runtime.template.TemplateCopier
import io.grpc.ConnectivityState
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.file.Files

class ServerRunner(
    private val serverVersionLoader: ServerVersionLoader,
    private val serverConfigurator: ServerConfiguratorExecutor,
    private val templateCopier: TemplateCopier,
    private val serverHost: ServerHost
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

    companion object {

        val DEFAULT_OPTIONS = listOf("-Dcom.mojang.eula.agree=true", "-jar")
        val DEFAULT_ARGUMENTS = listOf("nogui")
        val DEFAULT_EXECUTABLE: String = File(System.getProperty("java.home"), "bin/java").absolutePath

        fun getServerDir(server: Server): File {
            return getServerDir(server, GroupRuntime.Config.load<GroupRuntime>("${server.group}.yml"))
        }

        private fun getServerDir(server: Server, runtimeConfig: GroupRuntime?): File {
            var basicUrl = if (runtimeConfig?.parentDir != null) runtimeConfig.parentDir else "${server.group}/${server.group}-${server.numericalId}"
            if(!basicUrl.startsWith("/")) basicUrl = "${ServerRunnerPlaceholders.RUNNING_PATH}/$basicUrl"
            return File(basicUrl)
        }
    }

    fun startServer(server: Server): Boolean {
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
        val builder = buildProcess(server, runtimeConfig).inheritIO()
        if (!builder.directory().exists()) builder.directory().mkdirs()
        templateCopier.copy(server, TemplateActionType.DEFAULT)
        templateCopier.copy(server, TemplateActionType.RANDOM)
        if(!serverConfigurator.configurate(server)) {
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Failed to configure server.")
            Files.delete(getServerDir(server).toPath())
            return false
        }
        val process = builder.start()
        running[server] = process.toHandle()
        logger.info("Server ${server.uniqueId} of group ${server.group} now running on PID ${process.pid()}")
        return true
    }

    fun stopServer(server: Server): Boolean {
        if (!stopServer(server.uniqueId)) return false
        templateCopier.copy(server, TemplateActionType.SHUTDOWN)
        Files.delete(getServerDir(server).toPath())
        return true
    }

    private fun stopServer(uniqueId: String): Boolean {
        val server = getServer(uniqueId) ?: return false
        val process = running[server] ?: return false
        if (!process.destroy()) return false
        running.remove(server)
        return true
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
        val command = mutableListOf<String>()
        command.add(args.executable ?: DEFAULT_EXECUTABLE)
        if (!args.options.isNullOrEmpty()) command.addAll(args.options)
        command.add(serverVersionLoader.getServerJar(server).absolutePath)
        if (!args.arguments.isNullOrEmpty()) command.addAll(args.arguments)
        val builder = ProcessBuilder()
            .command(command)
            .directory(getServerDir(server, runtimeConfig))
        builder.environment()["HOST_IP"] = serverHost.host
        builder.environment()["HOST_PORT"] = serverHost.port.toString()
        builder.environment()["CONTROLLER_IP"] = System.getenv("CONTROLLER_IP") ?: "127.0.0.1"
        builder.environment()["CONTROLLER_PORT"] = System.getenv("CONTROLLER_PORT") ?: "5816"
        return builder
    }

    @OptIn(InternalCoroutinesApi::class)
    fun updateRunning(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (NonCancellable.isActive) {
                running.keys.forEach {
                    val process = running[it]
                    val realProcess = PortProcessHandle.of(it.port.toInt()).orElse(null)
                    if(realProcess != null) {
                        if(process?.pid() != realProcess.pid()) stopServer(it)
                    }else {
                        stopServer(it)
                    }
                }
                delay(5000L)
            }
        }
    }

}