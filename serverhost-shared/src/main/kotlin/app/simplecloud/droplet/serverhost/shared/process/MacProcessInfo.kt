package app.simplecloud.droplet.serverhost.shared.process

class MacProcessInfo(private val pid: Long) : ProcessInfo {
    override fun getCommand(): String {
        val cmd = Runtime.getRuntime().exec(arrayOf("ps", "-p", pid.toString(), "-o", "command="))
        return cmd.inputReader(Charsets.UTF_8).readText().trim()
    }

    override fun getRamAndCpuPercent(): Pair<Double, Double> {
        val cmd = Runtime.getRuntime()
            .exec(arrayOf("ps", "-p", pid.toString(), "-o", "%cpu,%mem="))
        var cpu = 0.0
        var mem = 0.0
        cmd.inputReader(Charsets.UTF_8).use { reader ->
            reader.readLines().forEach { line ->
                val resultString = line.trim()
                val result = resultString.split("\\s+".toRegex())
                if (result.size >= 2) {
                    cpu = result[0].toDoubleOrNull() ?: 0.0
                    mem = result[1].toDoubleOrNull() ?: 0.0
                }
            }
        }
        return Pair(mem, cpu)
    }

    override fun asHandle(): ProcessHandle {
        return ProcessHandle.of(pid).get()
    }
}
