package app.simplecloud.droplet.serverhost.runtime.config.environment

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class EnvironmentStartConfig(
    val command: List<String>? = null,
    val docker: DockerStartConfig? = null,
)