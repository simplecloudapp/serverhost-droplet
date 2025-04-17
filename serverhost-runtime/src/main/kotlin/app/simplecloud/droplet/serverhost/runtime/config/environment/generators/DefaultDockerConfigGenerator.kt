package app.simplecloud.droplet.serverhost.runtime.config.environment.generators

import app.simplecloud.droplet.serverhost.runtime.config.environment.*
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand

object DefaultDockerConfigGenerator : EnvironmentConfigGenerator {
    override fun generate(): EnvironmentConfig {
        return EnvironmentConfig(
            name = getName(),
            isDocker = true,
            buildPolicy = BuildPolicy.ONCE_AND_TRIGGER,
            imagePullPolicy = ImagePullPolicy.IF_NOT_PRESENT,
            start = EnvironmentStartConfig(
                command = listOf(
                    "java",
                    "-Xms%MIN_MEMORY%M",
                    "-Xmx%MAX_MEMORY%M",
                    "-Dcom.mojang.eula.agree=true",
                    "-jar",
                    "server.jar",
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