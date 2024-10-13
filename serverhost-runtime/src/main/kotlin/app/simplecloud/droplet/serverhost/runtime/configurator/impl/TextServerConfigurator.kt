package app.simplecloud.droplet.serverhost.runtime.configurator.impl

import app.simplecloud.droplet.serverhost.runtime.configurator.ServerConfigurator
import org.spongepowered.configurate.ConfigurationNode
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

object TextServerConfigurator : ServerConfigurator<String> {
    override fun load(data: ConfigurationNode): String? {
        return data.string
    }

    override fun load(file: File): String? {
        throw UnsupportedOperationException("Method is not implemented.")
    }

    override fun save(data: String, file: File) {
        val writer = BufferedWriter(FileWriter(file))
        writer.write(data)
        writer.close()
    }
}