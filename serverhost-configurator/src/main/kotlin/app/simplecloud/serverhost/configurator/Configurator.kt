package app.simplecloud.serverhost.configurator

import org.spongepowered.configurate.ConfigurationNode
import java.io.File

interface Configurator<T> {
    fun load(data: ConfigurationNode): T?
    fun save(data: T, file: File)
    fun load(file: File): T?
}

// This sadly has to exist due to kotlin not being able to infer the type on its own
fun <T> Configurator<T>.save(data: Any, file: File) {
    @Suppress("UNCHECKED_CAST")
    return this.save(data as T, file)
}