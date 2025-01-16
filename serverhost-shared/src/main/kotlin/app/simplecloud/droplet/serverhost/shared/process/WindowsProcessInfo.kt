package app.simplecloud.droplet.serverhost.shared.process

class WindowsProcessInfo(private val pid: Long) : ProcessInfo {
    override fun getCommand(): String {
        return ProcessHandle.of(pid).get().info().commandLine().get()
    }

    override fun getRamAndCpuPercent(): Pair<Double, Double> {
        TODO("Not yet implemented")
    }

    override fun asHandle(): ProcessHandle {
        return ProcessHandle.of(pid).get()
    }

}