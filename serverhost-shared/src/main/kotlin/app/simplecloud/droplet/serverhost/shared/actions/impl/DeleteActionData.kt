package app.simplecloud.droplet.serverhost.shared.actions.impl

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DeleteActionData(
    val path: String,
    val force: Boolean = true
)