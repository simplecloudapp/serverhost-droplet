package app.simplecloud.droplet.serverhost.shared.actions.impl.download

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DownloadActionData(
    val url: String,
    val path: String,
    val replace: Boolean = true,
    val initDirIfMissing: Boolean = true
)


