package app.simplecloud.droplet.serverhost.shared.logs


class ScreenExecutor(private var pid: Long) {

    private var streamingProcess: Process? = null
    private var isScreen = false

    init {
        var handle = ProcessHandle.of(pid)
        while (handle.isPresent) {
            if (handle.get().info().commandLine().orElseGet { "" }.lowercase().startsWith("screen")) {
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

    fun sendCommand(toSend: String): Process {
        //TODO: Test this on not arch
        val command = arrayOf("screen", "-S", pid.toString(), "-X", "stuff \"$toSend\\n\"")
        return Runtime.getRuntime().exec(command)
    }

    // Cancel the process explicitly if needed
    private fun stopHook(process: Process) {
        process.destroy()
    }

    fun stopHook() {
        stopHook(streamingProcess!!)
    }
}