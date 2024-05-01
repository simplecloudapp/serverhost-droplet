package app.simplecloud.droplet.serverhost.runtime.config

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapper
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.io.FileFilter
import java.nio.file.Path
import java.util.*
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.reflect.KClass

open class YamlConfig(private val dirPath: String) {

    inline fun <reified T> load(): T? {
        return load(null)
    }

    fun buildNode(path: String?): Pair<CommentedConfigurationNode, YamlConfigurationLoader> {
        val file = File(if (path != null) "${dirPath}/${path.lowercase()}" else dirPath)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        return buildNode(file.toPath())
    }

    private fun buildNode(path: Path): Pair<CommentedConfigurationNode, YamlConfigurationLoader> {
        val loader = YamlConfigurationLoader.builder()
            .path(path)
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(objectMapperFactory())
                }
            }.build()
        return Pair(loader.load(), loader)
    }

    inline fun <reified T> load(path: String?): T? {
        val node = buildNode(path).first
        return node.get<T>()
    }

    fun <T : Any> loadAll(type: KClass<T>, path: Path): MutableMap<String, T> {
        val list = mutableMapOf<String, T>()
        if (path.isDirectory()) {
            path.toFile().listFiles(FileFilter {
                it.name.endsWith(".yml") || it.isDirectory
            })?.forEach {
                list.putAll(loadAll(type, it.toPath()))
            }
        } else {
            val node = buildNode(path).first
            node.get(type)?.let { list[path.nameWithoutExtension] = it }
        }

        return list
    }

    inline fun <reified T : Any> loadAll(path: Path) = loadAll(T::class, path)


    fun load(yml: String): ConfigurationNode {
        return YamlConfigurationLoader.builder().buildAndLoadString(yml)
    }

    inline fun <reified T> loadYaml(yml: String): T? {
        val node = YamlConfigurationLoader.builder().defaultOptions {
            it.serializers { builder ->
                builder.registerAnnotatedObjects(objectMapperFactory())
            }
        }.buildAndLoadString(yml)
        return objectMapper<T>().load(node)
    }

    fun loadFile(file: File): String? {
        if (!file.exists()) return null
        val scanner = Scanner(file)
        var result = ""
        while (scanner.hasNextLine()) result += "${scanner.nextLine()}\n"
        return result
    }

    fun loadFile(path: String): String? {
        val file = File(dirPath, path)
        return loadFile(file)
    }
    open fun <T> save(path: String?, obj: T) {
        val pair = buildNode(path)
        pair.first.set(obj)
        pair.second.save(pair.first)
    }
}