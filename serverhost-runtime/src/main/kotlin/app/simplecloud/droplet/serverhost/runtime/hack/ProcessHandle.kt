package app.simplecloud.droplet.serverhost.runtime.hack

import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ServerDefinition
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object PortProcessHandle {

    private const val WINDOWS_PID_INDEX = 3
    private val windowsPattern = Pattern.compile("\\s*TCP\\s+\\S+:(\\d+)\\s+\\S+:(\\d+)\\s+\\S+\\s+(\\d+)")

    private val preBindPorts = ConcurrentHashMap<Int, LocalDateTime>()

    fun of(port: Int): Optional<ProcessHandle> {
        val os = OS.get() ?: return Optional.empty()
        val command = when (os) {
            OS.UNIX -> arrayOf("sh", "-c", "lsof -i :$port | awk '{print \$2}'")
            OS.WINDOWS -> arrayOf("cmd", "/c", "netstat -ano | findstr $port")
        }

        val process = Runtime.getRuntime().exec(command)

        val bufferedReader = process.inputReader()
        val processId = bufferedReader.useLines { lines ->
            lines.firstNotNullOfOrNull { parseProcessIdOrNull(os, it) }
        }

        if (processId == null) {
            return Optional.empty()
        }

        return ProcessHandle.of(processId)
    }

    private fun parseProcessIdOrNull(os: OS, line: String): Long? {
        return when (os) {
            OS.UNIX -> line.toLongOrNull()
            OS.WINDOWS -> {
                val matcher = windowsPattern.matcher(line)
                if (!matcher.matches()) {
                    return null
                }

                return matcher.group(WINDOWS_PID_INDEX).toLongOrNull()
            }
        }
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

    fun removePreBind(port: Int) {
        preBindPorts.remove(port)
    }

}