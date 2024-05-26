package app.simplecloud.droplet.serverhost.runtime.configurator

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.config.YamlConfig
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.io.File


@ConfigSerializable
data class ServerConfiguration(
    val dependsOn: List<String>,
    @Setting("paths")
    val operations: List<ServerConfigurationEntry>
)
