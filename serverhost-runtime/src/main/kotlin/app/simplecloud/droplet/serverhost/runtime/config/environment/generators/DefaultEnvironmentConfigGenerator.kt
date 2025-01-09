package app.simplecloud.droplet.serverhost.runtime.config.environment.generators

import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfig
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfigGenerator
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentStartConfig
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

object DefaultEnvironmentConfigGenerator : EnvironmentConfigGenerator {
    override fun generate(args: ServerHostStartCommand): EnvironmentConfig {
        val javaHome = Paths.get(System.getProperty("java.home"), "/bin/java").absolutePathString()
        val libs = args.libsPath.absolutePathString()
        return EnvironmentConfig(
            name = getName(),
            start = EnvironmentStartConfig(
                command = listOf(
                    javaHome,
                    "-Xms%MIN_MEMORY%M",
                    "-Xmx%MAX_MEMORY%M",
                    "-Dcom.mojang.eula.agree=true",
                    "-cp",
                    "${libs}/*${File.pathSeparator}%SERVER_FILE%",
                    "%MAIN_CLASS%",
                    "nogui"
                )
            )
        )
    }

    override fun getName(): String {
        return "default"
    }
}