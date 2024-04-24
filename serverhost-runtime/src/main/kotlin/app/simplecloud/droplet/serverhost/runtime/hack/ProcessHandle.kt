package app.simplecloud.droplet.serverhost.runtime.hack

import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ServerDefinition
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object PortProcessHandle {

    private val preBindPorts = ConcurrentHashMap<Int, LocalDateTime>()

    fun of(port: Int): Optional<ProcessHandle> {
        val os = OS.get() ?: return Optional.empty()
        val command = when (os) {
            OS.WINDOWS -> arrayOf("netstat -ano | findstr $port")
            OS.UNIX -> arrayOf("sh", "-c", "lsof -i :$port | awk '{print \$2}'")
        }

        val process = Runtime.getRuntime().exec(command)

        val bufferedReader = process.inputReader()
        val processId = bufferedReader.useLines { lines ->
            lines.firstNotNullOfOrNull { it.toLongOrNull() }
        }

        if (processId == null) {
            return Optional.empty()
        }

        return ProcessHandle.of(processId)
    }

    fun ofOld(port: Int): Optional<ProcessHandle> {
        val os = OS.get() ?: return Optional.empty()
        val command: String
        val pattern: Pattern
        val pidIndex: Int
        when (os) {
            OS.WINDOWS -> {
                command = "cmd /c netstat -a -n -o | findstr $port"
                pattern = Pattern.compile("\\s*TCP\\s+\\S+:(\\d+)\\s+\\S+:(\\d+)\\s+\\S+\\s+(\\d+)")
                pidIndex = 3
            }

            OS.UNIX -> {
                command = "bash -c lsof -i :$port"
                pattern = Pattern.compile("\\S+\\s+(\\d+)\\s+.*:$port")
                pidIndex = 1
            }
        }

        val process = Runtime.getRuntime().exec(command)
        val reader = process.inputReader()
        var line = ""
        while (reader.readLine()?.also { line = it } != null) {
            val matcher = pattern.matcher(line)
            if (matcher.matches()) {
                val pid: Long = matcher.group(pidIndex).toLong()
                return ProcessHandle.of(pid)
            }
        }
        return Optional.empty()
    }

    @Synchronized
    fun findNextFreePort(startPort: Int, serverDefinition: ServerDefinition): Int {
        val server = Server.fromDefinition(serverDefinition)
        var port = startPort
        val time = LocalDateTime.now()
        while (!of(port).isEmpty || time.isBefore(preBindPorts.getOrDefault(port, LocalDateTime.MIN))) {
            port++
        }
        addPreBind(port, time, server.properties.getOrDefault("max-startup-seconds", "120").toLong())
        return port
    }

    private fun addPreBind(port: Int, time: LocalDateTime, duration: Long) {
        preBindPorts[port] = time.plusSeconds(duration)
    }

}