package app.simplecloud.droplet.serverhost.runtime.configurator

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ServerConfigurationEntry(
    val type: ServerConfiguratorType,
    val path: String,
    val data: CommentedConfigurationNode,
)
