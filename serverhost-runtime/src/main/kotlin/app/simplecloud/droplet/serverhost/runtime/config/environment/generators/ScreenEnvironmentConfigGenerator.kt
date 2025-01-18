package app.simplecloud.droplet.serverhost.runtime.config.environment.generators

import app.simplecloud.droplet.serverhost.runtime.config.environment.*
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.util.ScreenCapabilities
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

object ScreenEnvironmentConfigGenerator : EnvironmentConfigGenerator {
    override fun generate(args: ServerHostStartCommand): EnvironmentConfig {
        val javaHome = Paths.get(System.getProperty("java.home"), "/bin/java").absolutePathString()
        val libs = args.libsPath.absolutePathString()
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
                "%SESSION_NAME%",
                javaHome,
                "-Xms%MIN_MEMORY%M",
                "-Xmx%MAX_MEMORY%M",
                "-Dcom.mojang.eula.agree=true",
                "-cp",
                "${libs}/*:%SERVER_FILE%",
                "%MAIN_CLASS%",
                "nogui"
            )
        )
        return EnvironmentConfig(
            name = getName(),
            isScreen = true,
            start = EnvironmentStartConfig(
                command = command
            ),
            update = EnvironmentUpdateConfig(
                command = listOf("screen", "-S", "%SESSION_NAME%", "-X")
            ),
            stop = EnvironmentStopConfig(
                command = listOf("screen", "-S", "%SESSION_NAME%", "-X", "quit"),
            )
        )
    }

    override fun getName(): String {
        return "screen"
    }
}