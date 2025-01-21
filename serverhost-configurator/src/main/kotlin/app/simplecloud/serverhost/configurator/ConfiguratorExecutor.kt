package app.simplecloud.serverhost.configurator

import app.simplecloud.serverhost.config.YamlConfig
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists

class ConfiguratorExecutor {
    fun configurate(
        configurable: Configurable,
        configurator: String,
        destination: File,
        forwardingSecret: String,
        replace: Boolean = true
    ): Boolean {
        val configLoader = YamlConfig("options/configurators")
        val content = configLoader.loadFile("$configurator.yml") ?: return false
        return configurateContent(configurable, content, destination, forwardingSecret, replace)
    }

    fun configurateFile(
        configurable: Configurable,
        configurator: Path,
        destination: File,
        forwardingSecret: String,
        replace: Boolean = true
    ): Boolean {
        val content = loadFile(configurator) ?: return false
        return configurateContent(configurable, content, destination, forwardingSecret, replace)
    }

    private fun configurateContent(
        configurable: Configurable,
        content: String,
        destination: File,
        forwardingSecret: String,
        replace: Boolean = true
    ): Boolean {
        val mappedContent = ConfiguratorPlaceholderMapper.map(content, configurable, forwardingSecret)
        val config = YamlConfig.loadYaml<ConfiguratorConfig>(mappedContent) ?: return false
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

    private fun loadFile(file: Path): String? {
        if (!file.exists()) return null
        val scanner = Scanner(file)
        var result = ""
        while (scanner.hasNextLine()) result += "${scanner.nextLine()}\n"
        return result
    }
}
