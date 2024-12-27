package app.simplecloud.droplet.serverhost.runtime.config.environment

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class EnvironmentConfig(
    val enabled: Boolean = true,
    val isScreen: Boolean = false,
    val useScreenStop: Boolean = false,
    val isDocker: Boolean = false,
    val name: String = "",
    val start: EnvironmentStartConfig? = null,
    val stop: EnvironmentStopConfig? = null
)