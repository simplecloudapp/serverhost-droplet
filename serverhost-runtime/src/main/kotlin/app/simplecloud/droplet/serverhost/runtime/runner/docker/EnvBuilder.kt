package app.simplecloud.droplet.serverhost.runtime.runner.docker

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.config.environment.DockerStartConfig
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand

class EnvBuilder(
    private val config: DockerStartConfig,
    private val args: ServerHostStartCommand,
    private val env: Map<String, String> = mapOf()
) {
    fun build(server: Server): List<String> {
        val result = mutableListOf<String>()
        if (config.envMappings.containsKey("forwarding-secret")) {
            result.add("${config.envMappings["forwarding-secret"]}=${args.forwardingSecret}")
        }
        if(config.envMappings.containsKey("type")) {
            result.add("${config.envMappings["type"]}=${server.type}")
        }
        env.forEach { (key, value) ->
            result.add("$key=$value")
        }
        return result
    }
}