package app.simplecloud.droplet.serverhost.shared.logs

import java.io.OutputStream


class ScreenExecutor(private var pid: Long) {

    private var streamingProcess: Process? = null
    private var isScreen = false

    init {
        var handle = ProcessHandle.of(pid)
        while(handle.isPresent) {
            if(handle.get().info().commandLine().orElseGet { "" }.startsWith("SCREEN")) {
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

    private fun startScreenProcess(toSend: String): Process {
        //TODO: Test this on not arch
        val command = arrayOf("screen", "-S", pid.toString(), "-X", "stuff \"$toSend\\n\"")
        return Runtime.getRuntime().exec(command)
    }

    // Send a command to the screen session
    fun sendCommand(command: String) {
        val process = startScreenProcess(command)

        // Get the input stream and write the command to it
        val outputStream: OutputStream = process.outputStream
        outputStream.write("$command\n".toByteArray())
        outputStream.flush()
        stopHook(process)
    }

    // Cancel the process explicitly if needed
    private fun stopHook(process: Process) {
        process.destroy()
    }

    fun stopHook() {
        stopHook(streamingProcess!!)
    }
}