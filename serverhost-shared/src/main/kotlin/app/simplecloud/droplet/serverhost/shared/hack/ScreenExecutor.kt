package app.simplecloud.droplet.serverhost.shared.hack

class ScreenExecutor(private var pid: Long) {
    private var isScreen = false

    init {
        var handle = ProcessHandle.of(pid)
        while (handle.isPresent) {
            if (handle.get().info().command().orElseGet { "" }.lowercase().startsWith("screen") || handle.get().info()
                    .arguments().orElseGet { arrayOf("none") }[0].lowercase().startsWith("screen")
            ) {
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