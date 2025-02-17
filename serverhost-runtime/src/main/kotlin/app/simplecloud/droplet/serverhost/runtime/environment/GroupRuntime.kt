package app.simplecloud.droplet.serverhost.runtime.environment

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class GroupRuntime(
    val environment: String? = null,
    val ignore: Boolean? = false,
    val parentDir: String? = null,
    val version: String = "1",
)
