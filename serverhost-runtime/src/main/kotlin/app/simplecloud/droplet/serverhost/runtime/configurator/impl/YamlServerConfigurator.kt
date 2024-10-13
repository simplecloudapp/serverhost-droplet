package app.simplecloud.droplet.serverhost.runtime.configurator.impl

import app.simplecloud.droplet.serverhost.runtime.configurator.ServerConfigurator
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

object YamlServerConfigurator : ServerConfigurator<ConfigurationNode> {
    override fun load(data: ConfigurationNode): ConfigurationNode {
        return data
    }

    override fun load(file: File): ConfigurationNode? {
        if (!file.exists()) return null
        val loader = YamlConfigurationLoader.builder().file(file).build()
        return loader.load()
    }

    override fun save(data: ConfigurationNode, file: File) {
        val existing = load(file)
        val saved: ConfigurationNode
        if (existing == null) {
            saved = data
        } else {
            saved = existing.copy()
            data.mergeFrom(saved)
        }
        val loader = YamlConfigurationLoader.builder().file(file).build()
        loader.save(saved)
    }
}