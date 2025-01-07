package app.simplecloud.droplet.serverhost.shared.actions.impl.download

import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Niklas Nieberler
 */

@ConfigSerializable
data class GithubDownloadActionData(
    val url: String = "",
    val path: String = "",
    val assetName: String = "",
    val releaseTag: String? = null
)