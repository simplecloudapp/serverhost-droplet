package app.simplecloud.droplet.serverhost.runtime.util

import java.io.IOException

data class ScreenCapabilities(
    val hasLogging: Boolean = false,
    val hasLogFile: Boolean = false
) {

    companion object {
        fun getScreenCapabilities(): ScreenCapabilities {
            return try {
                val process = ProcessBuilder("screen", "-h").start()
                val output = process.inputStream.bufferedReader().readText()

                ScreenCapabilities(
                    hasLogging = output.contains("-L") && output.contains("Turn on output logging"),
                    hasLogFile = output.contains("-Logfile") && output.contains("set logfile name")
                )
            } catch (e: IOException) {
                ScreenCapabilities()
            }
        }

        fun isScreenAvailable(): Boolean {
            return try {
                val process = ProcessBuilder("which", "screen").start()
                process.waitFor() == 0
            } catch (e: IOException) {
                false
            }
        }
    }

}