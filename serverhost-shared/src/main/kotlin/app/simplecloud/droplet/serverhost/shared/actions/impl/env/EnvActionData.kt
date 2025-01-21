package app.simplecloud.droplet.serverhost.shared.actions.impl.env

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class EnvActionData(
    val key: String,
    val value: String,
)
