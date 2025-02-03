package app.simplecloud.droplet.serverhost.runtime.config.environment

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DockerStartConfig(
    val image: String = "",
    val containerId: String = "",
)
