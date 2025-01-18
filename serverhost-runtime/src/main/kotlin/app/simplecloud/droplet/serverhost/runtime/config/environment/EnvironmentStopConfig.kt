package app.simplecloud.droplet.serverhost.runtime.config.environment

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class EnvironmentStopConfig(
    val command: List<String>? = null,
)