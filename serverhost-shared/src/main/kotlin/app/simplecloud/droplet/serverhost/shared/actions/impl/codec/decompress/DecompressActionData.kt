package app.simplecloud.droplet.serverhost.shared.actions.impl.codec.decompress

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DecompressActionData(
    val archive: String,
    val path: String = "/",
    val replace: Boolean = true
)
