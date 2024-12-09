package app.simplecloud.droplet.serverhost.shared.actions.impl.download.modrinth

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ModrinthDownloadActionData(
    val modId: String,
    val gameVersion: String,
    val loader: String,
    val destinationPath: String,
    val overwrite: Boolean = true,
    val initMissingDirectories: Boolean = true,
) {
}