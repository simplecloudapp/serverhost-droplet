package app.simplecloud.serverhost.configurator.impl

import app.simplecloud.serverhost.configurator.Configurator
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.io.File

object HoconConfigurator : Configurator<ConfigurationNode> {
    override fun load(data: ConfigurationNode): ConfigurationNode {
        return data
    }

    override fun load(file: File): ConfigurationNode? {
        if (!file.exists()) return null
        val loader = HoconConfigurationLoader.builder().file(file).build()
        return loader.load()
    }

    override fun save(data: ConfigurationNode, file: File) {
        val existing = load(file)
        val saved: ConfigurationNode = if (existing == null) {
            data
        } else {
            data.mergeFrom(existing.copy())
        }
        val loader = HoconConfigurationLoader.builder().file(file).build()
        loader.save(saved)
    }
}