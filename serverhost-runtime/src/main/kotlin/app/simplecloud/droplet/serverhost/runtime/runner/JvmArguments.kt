package app.simplecloud.droplet.serverhost.runtime.runner

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class JvmArguments(
    val executable: String?,
    val options: List<String>?,
    val arguments: List<String>?,
    var screenStop: String?
)