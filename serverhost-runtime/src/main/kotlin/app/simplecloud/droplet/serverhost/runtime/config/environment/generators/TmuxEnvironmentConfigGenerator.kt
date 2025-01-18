package app.simplecloud.droplet.serverhost.runtime.config.environment.generators

import app.simplecloud.droplet.serverhost.runtime.config.environment.*
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

object TmuxEnvironmentConfigGenerator : EnvironmentConfigGenerator {
    override fun generate(args: ServerHostStartCommand): EnvironmentConfig {
        val javaHome = Paths.get(System.getProperty("java.home"), "/bin/java").absolutePathString()
        val libs = args.libsPath.absolutePathString()
        return EnvironmentConfig(
            name = getName(),
            start = EnvironmentStartConfig(
                command = listOf(
                    "tmux",
                    "new-session",
                    "-d",
                    "-s",
                    "%SESSION_NAME%",
                    javaHome,
                    "-Xms%MIN_MEMORY%M",
                    "-Xmx%MAX_MEMORY%M",
                    "-Dcom.mojang.eula.agree=true",
                    "-cp",
                    "${libs}/*:%SERVER_FILE%",
                    "%MAIN_CLASS%",
                    "nogui",
                    "&&",
                    "tmux",
                    "pipe-pane",
                    "-t",
                    "%SESSION_NAME%",
                    "cat >> %LOG_FILE%"
                )
            ),
            update = EnvironmentUpdateConfig(
                command = listOf("tmux", "send-keys", "-t", "%SESSION_NAME%", "%COMMAND%", "C-m")
            ),
            stop = EnvironmentStopConfig(
                command = listOf("tmux", "kill-session", "-t", "%SESSION_NAME%"),
            )
        )
    }

    override fun getName(): String {
        return "default"
    }
}