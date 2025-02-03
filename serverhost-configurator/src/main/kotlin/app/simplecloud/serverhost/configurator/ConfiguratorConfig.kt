package app.simplecloud.serverhost.configurator

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting


@ConfigSerializable
data class ConfiguratorConfig(
    val dependsOn: List<String>,
    @Setting("paths")
    val operations: List<ConfigEntry>
)
