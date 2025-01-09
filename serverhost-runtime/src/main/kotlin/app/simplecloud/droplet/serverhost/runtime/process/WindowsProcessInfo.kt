package app.simplecloud.droplet.serverhost.runtime.process

class WindowsProcessInfo(private val pid: Long) : ProcessInfo {

    override fun getCommand(): String {
        return ProcessHandle.of(pid)
            .map { it.info().command().orElse("") }
            .orElse("")
    }

    override fun getRamAndCpuPercent(): Pair<Double, Double> {
        try {
            val memCmd = Runtime.getRuntime().exec(
                arrayOf(
                    "cmd.exe",
                    "/c",
                    "tasklist /FI \"PID eq $pid\" /FO CSV /NH /V"
                )
            )

            var memKB: Long
            memCmd.waitFor()
            memCmd.inputReader(Charsets.UTF_8).use { reader ->
                val output = reader.readText()
                memKB = output.split(",")
                    .getOrNull(4)
                    ?.trim()
                    ?.removeSurrounding("\"")
                    ?.replace(Regex("[^0-9]"), "") // Remove any non-numeric characters
                    ?.toLongOrNull() ?: 0L

            }

            val cpuCmd = Runtime.getRuntime().exec(
                arrayOf(
                    "cmd.exe",
                    "/c",
                    "tasklist /FI \"PID eq $pid\" /FO CSV /NH /V"
                )
            )

            var cpuPercent = 0.0
            cpuCmd.waitFor()
            cpuCmd.inputReader(Charsets.UTF_8).use { reader ->
                val output = reader.readText()
                val cpuTimeStr = output.split(",")
                    .getOrNull(8)
                    ?.trim()
                    ?.removeSurrounding("\"")
                    ?: "00:00:00"

                // Convert HH:MM:SS to seconds
                val parts = cpuTimeStr.split(":")
                val totalSeconds = if (parts.size == 3) {
                    (parts[0].toLongOrNull() ?: 0L) * 3600L +
                            (parts[1].toLongOrNull() ?: 0L) * 60L +
                            (parts[2].toLongOrNull() ?: 0L)
                } else {
                    0L
                }

                val uptime = ProcessHandle.of(pid)
                    .map { handle ->
                        handle.info().startInstant()
                            .map { start ->
                                java.time.Duration.between(start, java.time.Instant.now()).seconds
                            }
                            .orElse(1L)
                    }
                    .orElse(1L)
                    .coerceAtLeast(1L)

                val processors = Runtime.getRuntime().availableProcessors()
                cpuPercent = (totalSeconds.toDouble() / uptime.toDouble() * 100.0 * processors)
                    .coerceIn(0.0, 100.0)
            }

            val totalMemBytes = when (val osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean()) {
                is com.sun.management.OperatingSystemMXBean -> osBean.totalPhysicalMemorySize
                else -> Runtime.getRuntime().maxMemory()
            }

            val memBytes = memKB * 1024L
            val memPercent = (memBytes.toDouble() / totalMemBytes.toDouble() * 100.0)
                .coerceIn(0.0, 100.0)

            return Pair(memPercent, cpuPercent)
        } catch (e: Exception) {
            return Pair(0.0, 0.0)
        }
    }

    override fun asHandle(): ProcessHandle {
        return ProcessHandle.of(pid).get()
    }
}