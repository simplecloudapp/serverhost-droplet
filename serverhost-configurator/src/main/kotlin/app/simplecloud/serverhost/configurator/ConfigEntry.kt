package app.simplecloud.serverhost.configurator

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ConfigEntry(
    val type: ConfiguratorType,
    val path: String,
    val data: CommentedConfigurationNode,
)
