package app.simplecloud.droplet.serverhost.shared.actions.impl.placeholder

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class PlaceholderActionData(
    val key: String,
    val value: String,
    val lowercase: Boolean = false,
)
