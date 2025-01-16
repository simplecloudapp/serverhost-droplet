package app.simplecloud.droplet.serverhost.shared.hack

import app.simplecloud.droplet.serverhost.shared.process.ProcessInfo

class ScreenExecutor(private var pid: Long) {

    private var isScreen = false

    init {
        var handle = ProcessHandle.of(pid)
        while (handle.isPresent) {
            if (ProcessInfo.of(handle.get()).getCommand().lowercase().startsWith("screen")) {
                pid = handle.get().pid()
                isScreen = true
                break
            }
            handle = handle.get().parent()
        }
    }

    fun isScreen(): Boolean {
        return isScreen
    }

    fun sendCommand(toSend: Array<String>) {
        if (!isScreen) return
        val command = arrayOf("screen", "-S", pid.toString(), "-X", *toSend)
        Runtime.getRuntime().exec(command)
    }
}