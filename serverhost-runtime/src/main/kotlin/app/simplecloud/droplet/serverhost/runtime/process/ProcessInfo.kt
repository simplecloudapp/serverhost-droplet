package app.simplecloud.droplet.serverhost.runtime.process

import app.simplecloud.droplet.serverhost.shared.hack.OS
import app.simplecloud.droplet.serverhost.shared.hack.PortProcessHandle

interface ProcessInfo {
    fun getCommand(): String
    /**
     * Returns the ram of the current process
     * First: RAM
     * Second: CPU
     */
    fun getRamAndCpuPercent(): Pair<Double, Double>

    fun asHandle(): ProcessHandle

    companion object {
        fun ofPort(port: Long): ProcessInfo {
            val process = PortProcessHandle.of(port.toInt()).orElse(null) ?: throw NullPointerException("Port $port is not bound.")
            return of(process.pid())
        }

        fun of(handle: ProcessHandle): ProcessInfo {
            return of(handle.pid())
        }

        fun of(pid: Long): ProcessInfo {
            return when(OS.get()) {
                OS.WINDOWS -> {
                    WindowsProcessInfo(pid)
                }

                OS.UNIX -> {
                    UnixProcessInfo(pid)
                }

                null -> throw IllegalArgumentException("Unknown OS")
            }
        }
    }
}