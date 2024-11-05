package app.simplecloud.droplet.serverhost.shared.actions.impl

import app.simplecloud.droplet.serverhost.shared.actions.path.ActionPath
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CopyActionData(
    val from: ActionPath,
    val to: ActionPath,
)