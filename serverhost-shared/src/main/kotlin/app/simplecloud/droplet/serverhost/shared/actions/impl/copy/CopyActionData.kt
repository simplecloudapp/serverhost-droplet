package app.simplecloud.droplet.serverhost.shared.actions.impl.copy

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CopyActionData(
    val from: String,
    val to: String,
    val replace: Boolean = true,
    val initDirIfMissing: Boolean = false
)