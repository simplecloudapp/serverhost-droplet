package app.simplecloud.droplet.serverhost.shared.actions.impl.download

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DownloadActionData(
    val url: String,
    val destinationPath: String,
    val overwrite: Boolean = true,
    val initMissingDirectories: Boolean = true
)


