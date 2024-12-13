package app.simplecloud.droplet.serverhost.shared.actions.impl.codec.decompress

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DecompressActionData(
    val path: String,
    val dest: String,
    val replace: Boolean = true
)
