package app.simplecloud.droplet.serverhost.runtime.config.environment.generators

import app.simplecloud.droplet.serverhost.runtime.config.environment.DockerStartConfig
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfig
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfigGenerator
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentStartConfig
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand

object DefaultDockerConfigGenerator : EnvironmentConfigGenerator {
    override fun generate(args: ServerHostStartCommand): EnvironmentConfig {

        return EnvironmentConfig(
            name = getName(),
            isDocker = true,
            start = EnvironmentStartConfig(
                command = listOf(
                    "java",
                    "-Xms%MIN_MEMORY%M",
                    "-Xmx%MAX_MEMORY%M",
                    "-Dcom.mojang.eula.agree=true",
                    "-cp",
                    "/minecraft/libraries/*:server.jar",
                    "%MAIN_CLASS%",
                    "nogui"
                ),
                docker = DockerStartConfig()
            )
        )
    }

    override fun getName(): String {
        return "docker"
    }
}