package app.simplecloud.droplet.serverhost.shared.logs

import app.simplecloud.droplet.serverhost.shared.hack.ScreenExecutor


class ScreenCommandExecutor(pid: Long) {

    private val executor = ScreenExecutor(pid)

    fun isScreen(): Boolean {
        return executor.isScreen()
    }

    fun sendCommand(toSend: String) {
        executor.sendCommand(arrayOf("stuff", "$toSend\\n"))
    }
}