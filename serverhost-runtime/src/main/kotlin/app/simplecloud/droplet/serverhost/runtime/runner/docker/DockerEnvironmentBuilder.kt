package app.simplecloud.droplet.serverhost.runtime.runner.docker

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.config.environment.DockerStartConfig
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfig
import app.simplecloud.droplet.serverhost.runtime.host.ServerVersionLoader
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.EnvironmentBuilder
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntime
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntimeDirectory
import app.simplecloud.droplet.serverhost.runtime.util.JarMainClass

class DockerEnvironmentBuilder(
    private val config: EnvironmentConfig,
    private val args: ServerHostStartCommand,
    private val serverHost: ServerHost,
) : EnvironmentBuilder<List<String>> {

    override fun createRuntimePlaceholders(server: Server): MutableMap<String, String> {
        val placeholders = mutableMapOf(
            "%MIN_MEMORY%" to server.minMemory.toString(),
            "%MAX_MEMORY%" to server.maxMemory.toString(),
            "%MAIN_CLASS%" to JarMainClass.find(ServerVersionLoader.getAndDownloadServerJar(server.properties["server-url"] ?: "").toPath())
        )
        placeholders.putAll(server.properties.map {
            "%${it.key.uppercase().replace("-", "_")}%" to it.value
        })
        return placeholders
    }

    override fun buildRuntime(server: Server, repository: GroupRuntimeDirectory): GroupRuntime {
        val runtime = GroupRuntime("docker")
        repository.save("${server.group}.yml", runtime)
        return runtime
    }

    override fun buildEnv(server: Server): List<String> {
        val result = mutableListOf<String>()
        val dockerConf = config.start?.docker ?: DockerStartConfig()
        server.toEnv().forEach {
            if (dockerConf.envMappings.containsKey(it.key))
                result.add("${dockerConf.envMappings[it.key]}=${it.value}")
            result.add("${it.key}=${it.value}")
        }
        if (dockerConf.envMappings.containsKey("FORWARDING_SECRET"))
            result.add("${dockerConf.envMappings["FORWARDING_SECRET"]}=${args.forwardingSecret}")
        result.add("FORWARDING_SECRET=${args.forwardingSecret}")
        getDefaultEnv(serverHost, args).forEach { (key, value) ->
            result.add("$key=$value")
        }
        result.add("SIMPLECLOUD_COMMAND=${
            config.start?.command?.toMutableList()
                ?.let {
                    addAllWithPlaceholders(
                        it,
                        createRuntimePlaceholders(server)
                    ); return@let it.joinToString(" ")
                } ?: ""
        }")
        return result
    }
}