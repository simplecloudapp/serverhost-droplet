package app.simplecloud.droplet.serverhost.runtime.configurator

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.config.YamlConfig
import app.simplecloud.droplet.serverhost.runtime.configurator.impl.*
import app.simplecloud.droplet.serverhost.runtime.configurator.impl.TomlServerConfigurator
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.io.File
import java.nio.file.Files


@ConfigSerializable
data class ServerConfigurationEntry(
    val type: ServerConfigurationType,
    val path: String,
    val data: CommentedConfigurationNode,
)

@ConfigSerializable
data class ServerConfiguration(
    val dependsOn: List<String>,
    @Setting("paths")
    val operations: List<ServerConfigurationEntry>
)

enum class ServerConfigurationType(val configurator: ServerConfigurator<*>) {
    YML(YamlServerConfigurator),
    PROPERTIES(PropertiesServerConfigurator),
    TOML(TomlServerConfigurator),
    JSON(JsonServerConfigurator),
    TXT(TextServerConfigurator);
}

object ServerConfiguratorPlaceholderMapper {

    private fun toTemplateList(server: Server): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["ip"] = server.ip
        map["port"] = server.port
        map.putAll(server.properties)
        return map
    }

    fun map(templatedString: String, server: Server): String {
        var result = templatedString
        for ((template, value) in toTemplateList(server)) {
            result = result.replace("%$template%", value.toString())
        }
        return result
    }
}

class ServerConfiguratorExecutor {
    fun configurate(server: Server, runner: ServerRunner): Boolean {
        val configurator = server.properties["configurator"] ?: return false
        val configLoader = YamlConfig("options/configurators")
        val content = configLoader.loadFile("$configurator.yml") ?: return false
        val mappedContent = ServerConfiguratorPlaceholderMapper.map(templatedString = content, server = server)
        val config = configLoader.loadYaml<ServerConfiguration>(mappedContent) ?: return false
        config.operations.forEach {
            val data = it.type.configurator.load(it.data) ?: return false
            it.type.configurator.save(data, File(runner.getServerDir(server), it.path))
        }
        return true
    }

    //TODO: Move to util class
    fun copyDefaults() {
        copyDefault("options/configurators/bungeecord.yml")
        copyDefault("options/configurators/paper_velocity.yml")
        copyDefault("options/configurators/spigot.yml")
        copyDefault("options/configurators/spigot_standalone.yml")
        copyDefault("options/configurators/velocity.yml")
    }

    //TODO: Move to util class
    private fun copyDefault(path: String) {
        val file = File(path)
        if (!file.exists()) {
            Files.createDirectories(file.parentFile.toPath())
            Files.copy(ServerConfiguratorExecutor::class.java.getResourceAsStream("/$path")!!, File(path).toPath())
        }
    }
}

interface ServerConfigurator<T> {
    fun load(data: ConfigurationNode): T?
    fun save(data: T, file: File)
    fun load(file: File): T?
}

fun <T> ServerConfigurator<T>.save(data: Any, file: File) {
    return this.save(data as T, file)
}