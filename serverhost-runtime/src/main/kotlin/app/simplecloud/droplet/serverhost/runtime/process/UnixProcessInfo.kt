package app.simplecloud.droplet.serverhost.runtime.process

class UnixProcessInfo(private val pid: Long) : ProcessInfo {
    override fun getCommand(): String {
        val cmd = Runtime.getRuntime().exec(arrayOf("ps", "-wo", "cmd", "--no-headers", "-p", pid.toString()))
        return cmd.inputReader(Charsets.UTF_8).readText().trim()
    }

    override fun getRamAndCpuPercent(): Pair<Double, Double> {
        val cmd = Runtime.getRuntime()
            .exec(arrayOf("ps", "-wo", "%cpu,%mem", "--no-headers", "--forest", "-p", pid.toString()))
        var cpu = 0.0
        var mem = 0.0
        cmd.inputReader(Charsets.UTF_8).use { reader ->
            reader.readLines().forEach { line ->
                val resultString = line.trim()
                val result = resultString.split(" ")
                cpu = result.firstOrNull()?.toDouble() ?: 0.0
                mem = result.lastOrNull()?.toDouble() ?: 0.0
            }
        }
        return Pair(cpu, mem)
    }

    override fun asHandle(): ProcessHandle {
        return ProcessHandle.of(pid).get()
    }

}