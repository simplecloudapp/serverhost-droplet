package app.simplecloud.droplet.serverhost.runtime.hack

import java.time.LocalDateTime
import java.util.*
import java.util.regex.Pattern

class PortProcessHandle {
    companion object {

        private val preBindPorts = mutableMapOf<Int, LocalDateTime>()

        fun of(port: Int): Optional<ProcessHandle> {
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

        fun findNextFreePort(startPort: Int): Int {
            var port = startPort
            while(!of(port).isEmpty || LocalDateTime.now().isBefore(preBindPorts.getOrDefault(port, LocalDateTime.now().minusSeconds(1)))) {
                port++
            }
            return port
        }

        fun addPreBind(port: Int, time: LocalDateTime, duration: Long) {
            preBindPorts[port] = time.plusSeconds(duration)
        }
    }
}