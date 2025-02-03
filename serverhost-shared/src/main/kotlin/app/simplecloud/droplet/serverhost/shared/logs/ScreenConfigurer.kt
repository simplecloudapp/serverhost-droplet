package app.simplecloud.droplet.serverhost.shared.logs

import app.simplecloud.droplet.serverhost.shared.hack.ScreenExecutor

class ScreenConfigurer(pid: Long) {
    private val executor = ScreenExecutor(pid)

    fun setLogsFlush(interval: Int) {
        executor.sendCommand(arrayOf("logfile", "flush", interval.toString()))
    }
}