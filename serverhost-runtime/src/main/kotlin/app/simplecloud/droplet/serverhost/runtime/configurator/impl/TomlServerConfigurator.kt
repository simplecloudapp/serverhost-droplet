package app.simplecloud.droplet.serverhost.runtime.configurator.impl

import app.simplecloud.droplet.serverhost.runtime.configurator.ServerConfigurator
import org.spongepowered.configurate.ConfigurationNode
import java.io.File

object TomlServerConfigurator : ServerConfigurator<ConfigurationNode> {
    override fun load(data: ConfigurationNode): ConfigurationNode {
        return data
    }

    override fun load(file: File): ConfigurationNode? {
        TODO("Not yet implemented")
    }

    override fun save(data: ConfigurationNode, file: File) {
        TODO("Not yet implemented")
    }
}