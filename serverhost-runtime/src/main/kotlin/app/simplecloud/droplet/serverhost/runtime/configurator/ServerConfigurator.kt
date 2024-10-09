package app.simplecloud.droplet.serverhost.runtime.configurator

import org.spongepowered.configurate.ConfigurationNode
import java.io.File

interface ServerConfigurator<T> {
    fun load(data: ConfigurationNode): T?
    fun save(data: T, file: File)
    fun load(file: File): T?
}

// This sadly has to exist due to kotlin not being able to infer the type on its own
fun <T> ServerConfigurator<T>.save(data: Any, file: File) {
    @Suppress("UNCHECKED_CAST")
    return this.save(data as T, file)
}