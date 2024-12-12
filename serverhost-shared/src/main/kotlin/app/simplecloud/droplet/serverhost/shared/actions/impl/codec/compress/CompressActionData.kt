package app.simplecloud.droplet.serverhost.shared.actions.impl.codec.compress

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CompressActionData(
    val directory: String,
    val dest: String = "",
    val format: String = "zip",
    val replace: Boolean = false,
) {
}