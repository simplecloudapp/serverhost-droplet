package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntimeConfig
import app.simplecloud.droplet.serverhost.runtime.runner.JvmArguments
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.charset.Charset

class ServerRunner(
    private val serverVersionLoader: ServerVersionLoader
) {

    private val logger = LogManager.getLogger(ServerHostRuntime::class.java)

    private val running = mutableMapOf<String, ProcessHandle>()
    companion object {
        val DEFAULT_OPTIONS = listOf("-Dcom.mojang.eula.agree=true")
        val DEFAULT_ARGUMENTS = listOf("nogui")
        val DEFAULT_EXECUTABLE: String = File(System.getProperty("java.home"), "bin/java").absolutePath
    }
    fun startServer(server: Server) : Boolean  {
        if(running.containsKey(server.uniqueId)){
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Server with this id already exists.")
            return false
        }
        val runtimeConfig = GroupRuntimeConfig.load(server.group)
        if(!checkAcceptance(runtimeConfig)) {
            logger.error("Server ${server.uniqueId} of group ${server.group} failed to start: Group not supported by this ServerHost.")
            return false
        }
        serverVersionLoader.download(server)
        val builder = buildProcess(server, runtimeConfig)
        if(!builder.directory().exists()) builder.directory().mkdirs()
        logger.info(builder.directory().absolutePath)
        logger.info(builder.command().joinToString(" "))
        val process = builder.start()
        running[server.uniqueId] = process.toHandle()
        server.properties["pid"] = process.pid().toString()
        logger.info("Server ${server.uniqueId} of group ${server.group} now running on PID ${process.pid()}")
        return true
    }
    fun stopServer(server: Server) : Boolean {
        return stopServer(server.uniqueId)
    }

    fun stopServer(uniqueId: String) : Boolean {
        if(!running.containsKey(uniqueId)) return false
        val process = running[uniqueId] ?: return false
        if(!process.destroy()) return false
        running.remove(uniqueId)
        return true
    }
    fun reattachServer(server: Server) : Boolean {
        if(server.properties["pid"] == null || running.containsKey(server.uniqueId)) return false
        running[server.uniqueId] = ProcessHandle.of(server.properties["pid"]!!.toLong()).orElseThrow()
        return true
    }
    private fun checkAcceptance(runtimeConfig: GroupRuntimeConfig?): Boolean {
        return runtimeConfig == null || runtimeConfig.ignore != true
    }

    private fun getServerDir(server: Server, runtimeConfig: GroupRuntimeConfig?): File {
        return File(if(runtimeConfig?.parentDir != null) runtimeConfig.parentDir else "running/${server.group}/${server.uniqueId}")
    }
    private fun buildProcess(server: Server, runtimeConfig: GroupRuntimeConfig?): ProcessBuilder {
        val args: JvmArguments = if(runtimeConfig?.jvm != null) runtimeConfig.jvm else JvmArguments(DEFAULT_EXECUTABLE, DEFAULT_OPTIONS, DEFAULT_ARGUMENTS)
        val command = mutableListOf<String>()
        command.add(args.executable ?: DEFAULT_EXECUTABLE)
        if(!args.options.isNullOrEmpty()) command.addAll(args.options)
        command.add("-jar")
        command.add(serverVersionLoader.getServerJar(server).absolutePath)
        if(!args.arguments.isNullOrEmpty()) command.addAll(args.arguments)
        return ProcessBuilder()
            .command(command)
            .directory(getServerDir(server, runtimeConfig))
    }

}