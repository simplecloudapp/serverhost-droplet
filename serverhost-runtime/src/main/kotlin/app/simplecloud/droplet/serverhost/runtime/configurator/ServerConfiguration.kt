package app.simplecloud.droplet.serverhost.runtime.configurator

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting


@ConfigSerializable
data class ServerConfiguration(
    val dependsOn: List<String>,
    @Setting("paths")
    val operations: List<ServerConfigurationEntry>
)
