package app.simplecloud.droplet.serverhost.runtime.configurator.impl

import app.simplecloud.droplet.serverhost.runtime.configurator.ServerConfigurator
import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import java.io.File

object TomlServerConfigurator : ServerConfigurator<MutableMap<String, Any>> {
    override fun load(data: ConfigurationNode): MutableMap<String, Any> {
        val parsedData =
            data.childrenMap().map { it.key.toString() to (it.value.get<Any>() ?: Object()) }.toMap().toMutableMap()
        val map = mutableMapOf<String, Any>()
        map.tomlCombine(parsedData)
        return map
    }

    override fun load(file: File): MutableMap<String, Any>? {
        if (!file.exists()) return mutableMapOf()
        return Toml().read(file).toMap()
    }

    override fun save(data: MutableMap<String, Any>, file: File) {
        val existing = load(file) ?: mutableMapOf()
        val mergedMap = mergeMaps(existing, data)
        val writer = TomlWriter()
        writer.write(mergedMap, file)
    }

    private fun mergeMaps(first: Map<String, Any>, second: Map<String, Any>): Map<String, Any> {
        val result = first.toMutableMap()

        for ((key, secondValue) in second) {
            val firstValue = result[key]
            if (firstValue is Map<*, *> && secondValue is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                result[key] = mergeMaps(firstValue as Map<String, Any>, secondValue as Map<String, Any>)
            } else {
                result[key] = secondValue
            }
        }

        return result
    }

    // TODO: do we need this?
    private fun MutableMap<String, Any>.tomlCombine(map: MutableMap<String, Any>) {
        map.keys.forEach {
            if (!this.containsKey(it)) {
                this[it] = map[it]!!
                return@forEach
            }
            if (this[it] !is MutableMap<*, *>) {
                this[it] = map[it]!!
                return@forEach
            }
            val asMapThis: MutableMap<String, Any>
            val asMapOther: MutableMap<String, Any>
            try {
                asMapThis = this[it] as MutableMap<String, Any>
                asMapOther = map[it] as MutableMap<String, Any>
            } catch (e: Exception) {
                this[it] = map[it]!!
                return@forEach
            }

            this[it] = asMapThis.tomlCombine(asMapOther)
        }
    }
}