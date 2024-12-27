package app.simplecloud.droplet.serverhost.runtime.runner

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class GroupRuntime(
    val version: String = "1",
    val environment: String? = null,
    val ignore: Boolean? = false,
    val parentDir: String? = null
)
