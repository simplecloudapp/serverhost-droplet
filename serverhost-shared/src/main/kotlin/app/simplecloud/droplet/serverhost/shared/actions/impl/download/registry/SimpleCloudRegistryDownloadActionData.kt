package app.simplecloud.droplet.serverhost.shared.actions.impl.download.registry

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SimpleCloudRegistryDownloadActionData(
    val appSlug: String,
    val platform: String,
    val arch: String,
    val platformVersion: String? = null,
    val version: String? = null,
    val path: String,
    val update: Boolean = false,
    val asFile: Boolean = false,
    val initDirIfMissing: Boolean = true,
)
