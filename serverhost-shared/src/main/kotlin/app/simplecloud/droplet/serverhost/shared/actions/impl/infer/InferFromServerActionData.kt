package app.simplecloud.droplet.serverhost.shared.actions.impl.infer

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class InferFromServerActionData(
    val field: String,
    val key: String,
    val lowercase: Boolean = false,
)
