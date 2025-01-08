package app.simplecloud.droplet.serverhost.shared.logs

import app.simplecloud.droplet.serverhost.shared.hack.ScreenExecutor


class ScreenCommandExecutor(pid: Long) {

    private val executor = ScreenExecutor(pid)

    fun sendCommand(toSend: String) {
        executor.sendCommand(arrayOf("stuff", "$toSend\\n"))
    }
}