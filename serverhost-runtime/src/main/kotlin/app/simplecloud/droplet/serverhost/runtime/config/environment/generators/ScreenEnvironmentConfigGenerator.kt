package app.simplecloud.droplet.serverhost.runtime.config.environment.generators

import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfig
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfigGenerator
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentStartConfig
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.util.ScreenCapabilities
import app.simplecloud.droplet.serverhost.shared.hack.OS
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

object ScreenEnvironmentConfigGenerator : EnvironmentConfigGenerator {
    override fun generate(): EnvironmentConfig {
        val javaHome = Paths.get(System.getProperty("java.home"), "/bin/java").absolutePathString()
        val command = mutableListOf<String>()
        command.add("screen")
        val capabilities = ScreenCapabilities.getScreenCapabilities()
        if (capabilities.hasLogging) {
            command.add("-L")
            if (capabilities.hasLogFile) {
                command.add("-Logfile")
                command.add("%LOG_FILE%")
            }
        }
        command.addAll(
            listOf(
                "-dmS",
                "%SCREEN_NAME%",
                javaHome,
                "-Xms%MIN_MEMORY%M",
                "-Xmx%MAX_MEMORY%M",
                "-Dcom.mojang.eula.agree=true",
                "-jar",
                "%SERVER_FILE%",
                "nogui"
            )
        )
        return EnvironmentConfig(
            name = getName(),
            isScreen = true,
            useScreenStop = OS.get() != OS.WINDOWS,
            start = EnvironmentStartConfig(
                command = command
            )
        )
    }

    override fun getName(): String {
        return "screen"
    }
}