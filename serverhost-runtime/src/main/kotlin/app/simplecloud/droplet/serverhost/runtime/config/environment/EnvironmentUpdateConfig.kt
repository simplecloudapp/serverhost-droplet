package app.simplecloud.droplet.serverhost.runtime.config.environment

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class EnvironmentUpdateConfig(
    val command: List<String>? = null,
)
