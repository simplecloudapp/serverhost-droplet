package app.simplecloud.droplet.serverhost.shared.actions.impl.conditional

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ConditionalActionData(
    val first: String,
    val second: String,
    val matcher: String,
    val executes: String,
)