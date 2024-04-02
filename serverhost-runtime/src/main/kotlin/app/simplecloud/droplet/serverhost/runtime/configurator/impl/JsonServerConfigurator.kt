package app.simplecloud.droplet.serverhost.runtime.configurator.impl

import app.simplecloud.droplet.serverhost.runtime.configurator.ServerConfigurator
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.gson.GsonConfigurationLoader
import java.io.File

object JsonServerConfigurator : ServerConfigurator<ConfigurationNode> {
    override fun load(data: ConfigurationNode): ConfigurationNode {
        return data
    }

    override fun load(file: File): ConfigurationNode? {
        if (!file.exists()) return null
        val loader = GsonConfigurationLoader.builder().file(file).build()
        return loader.load()
    }

    override fun save(data: ConfigurationNode, file: File) {
        val existing = load(file)
        val saved: ConfigurationNode
        if (existing == null) {
            saved = data
        } else {
            saved = existing.copy()
            saved.mergeFrom(data)
        }
        val loader = GsonConfigurationLoader.builder().file(file).build()
        loader.save(saved)
    }
}