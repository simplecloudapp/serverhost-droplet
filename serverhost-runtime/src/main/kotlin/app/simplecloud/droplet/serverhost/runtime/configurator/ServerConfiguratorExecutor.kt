package app.simplecloud.droplet.serverhost.runtime.configurator

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.runtime.config.YamlConfig
import java.io.File

class ServerConfiguratorExecutor {
    fun configurate(server: Server, runner: ServerRunner, forwardingSecret: String): Boolean {
        val configurator = server.properties["configurator"] ?: return false
        val configLoader = YamlConfig("options/configurators")
        val content = configLoader.loadFile("$configurator.yml") ?: return false
        val mappedContent = ServerConfiguratorPlaceholderMapper.map(templatedString = content, server = server, forwardingSecret)
        val config = configLoader.loadYaml<ServerConfiguration>(mappedContent) ?: return false
        config.operations.forEach {
            val data = it.type.configurator.load(it.data) ?: return false
            it.type.configurator.save(data, File(runner.getServerDir(server), it.path))
        }
        return true
    }
}
