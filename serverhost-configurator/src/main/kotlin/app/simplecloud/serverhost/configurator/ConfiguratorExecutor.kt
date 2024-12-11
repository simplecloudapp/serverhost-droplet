package app.simplecloud.serverhost.configurator

import app.simplecloud.serverhost.config.YamlConfig
import java.io.File

class ConfiguratorExecutor {
    fun configurate(configurable: Configurable, configurator: String, destination: File, forwardingSecret: String, replace: Boolean = true): Boolean {
        val configLoader = YamlConfig("options/configurators")
        val content = configLoader.loadFile("$configurator.yml") ?: return false
        val mappedContent = ConfiguratorPlaceholderMapper.map(content, configurable, forwardingSecret)
        val config = configLoader.loadYaml<ConfiguratorConfig>(mappedContent) ?: return false
        config.operations.forEach {
            val data = it.type.configurator.load(it.data) ?: return false
            val dest = File(destination, it.path)
            if (dest.exists() && !replace) {
                return@forEach
            }
            it.type.configurator.save(data, dest)
        }
        return true
    }
}
