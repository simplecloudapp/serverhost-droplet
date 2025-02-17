package app.simplecloud.droplet.serverhost.shared.actions.impl.delete

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DeleteActionData(
    val path: String,
    val force: Boolean = true
)