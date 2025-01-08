package app.simplecloud.droplet.serverhost.shared.hack

import org.apache.logging.log4j.LogManager

class ScreenExecutor(private var pid: Long) {

    private val logger = LogManager.getLogger(ScreenExecutor::class.java)

    init {
        var handle = ProcessHandle.of(pid)
        while (handle.isPresent) {
            if (handle.get().info().commandLine().orElseGet { "" }.lowercase().startsWith("screen") || handle.get()
                    .info().command().orElseGet { "" }.lowercase().startsWith("screen") || handle.get().info()
                    .arguments().orElseGet { arrayOf("none") }.firstOrNull()?.lowercase()?.startsWith("screen") == true
            ) {
                val newPid = handle.get().pid()
                this.logger.info("Change pid $pid to $newPid")
                pid = newPid
                break
            }
            handle = handle.get().parent()
        }
    }

    fun sendCommand(toSend: Array<String>) {
        this.logger.info("Send command ${toSend.joinToString(" ")} to pid $pid")
        val command = arrayOf("screen", "-S", pid.toString(), "-X", *toSend)
        Runtime.getRuntime().exec(command)
    }
}