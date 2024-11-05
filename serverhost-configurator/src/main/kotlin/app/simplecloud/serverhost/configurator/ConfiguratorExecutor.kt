package app.simplecloud.serverhost.configurator

import app.simplecloud.serverhost.config.YamlConfig
import java.io.File

class ConfiguratorExecutor {
    fun configurate(configurable: Configurable, configurator: String, destination: File, forwardingSecret: String): Boolean {
        val configLoader = YamlConfig("options/configurators")
        val content = configLoader.loadFile("$configurator.yml") ?: return false
        val mappedContent = ConfiguratorPlaceholderMapper.map(content, configurable, forwardingSecret)
        val config = configLoader.loadYaml<ConfiguratorConfig>(mappedContent) ?: return false
        config.operations.forEach {
            val data = it.type.configurator.load(it.data) ?: return false
            it.type.configurator.save(data, File(destination, it.path))
        }
        return true
    }
}
