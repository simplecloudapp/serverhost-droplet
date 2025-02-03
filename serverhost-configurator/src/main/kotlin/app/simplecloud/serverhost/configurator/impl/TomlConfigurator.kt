package app.simplecloud.serverhost.configurator.impl

import app.simplecloud.serverhost.configurator.Configurator
import com.electronwill.nightconfig.core.file.FileConfig
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import java.io.File

object TomlConfigurator : Configurator<MutableMap<String, Any>> {
    override fun load(data: ConfigurationNode): MutableMap<String, Any> {
        val parsedData =
            data.childrenMap().map { it.key.toString() to (it.value.get<Any>() ?: Object()) }.toMap().toMutableMap()
        val map = mutableMapOf<String, Any>()
        map.tomlCombine(parsedData)
        return map
    }

    override fun load(file: File): MutableMap<String, Any>? {
        if (!file.exists()) return null
        val config = FileConfig.of(file)
        config.load()
        val returned = mutableMapOf<String, Any>()
        config.entrySet().forEach { entry ->
            returned[entry.key] = entry.getValue()
        }
        return returned
    }

    override fun save(data: MutableMap<String, Any>, file: File) {
        val existing = load(file) ?: mutableMapOf()
        val mergedMap = mergeMaps(existing, data)
        val config = FileConfig.builder(file).sync().build()
        config.bulkUpdate {
            mergedMap.forEach { (key, value) ->
                it.set(key, value)
            }
        }
        config.save()
        config.close()
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
                @Suppress("UNCHECKED_CAST")
                asMapThis = this[it] as MutableMap<String, Any>
                @Suppress("UNCHECKED_CAST")
                asMapOther = map[it] as MutableMap<String, Any>
            } catch (e: Exception) {
                this[it] = map[it]!!
                return@forEach
            }

            this[it] = asMapThis.tomlCombine(asMapOther)
        }
    }
}