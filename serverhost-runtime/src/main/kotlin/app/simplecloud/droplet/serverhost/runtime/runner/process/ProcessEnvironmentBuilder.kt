package app.simplecloud.droplet.serverhost.runtime.runner.process

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.EnvironmentBuilder
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntime
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntimeDirectory
import app.simplecloud.droplet.serverhost.runtime.util.ScreenCapabilities

class ProcessEnvironmentBuilder(private val serverHost: ServerHost, private val args: ServerHostStartCommand) :
    EnvironmentBuilder<Map<String, String>> {
    override fun buildEnv(server: Server): Map<String, String> {
        val result = mutableMapOf<String, String>()
        result.putAll(getDefaultEnv(serverHost, args))
        result.putAll(server.toEnv())
        return result
    }

    override fun createRuntimePlaceholders(server: Server): Map<String, String> {
        val placeholders = mutableMapOf(
            "%MIN_MEMORY%" to server.minMemory.toString(),
            "%MAX_MEMORY%" to server.maxMemory.toString(),
            "%SCREEN_NAME%" to "${server.group}-${server.numericalId}-${server.uniqueId.substring(0, 6)}",
            "%NUMERICAL_ID%" to server.numericalId.toString(),
            "%GROUP%" to server.group,
            "%UNIQUE_ID%" to server.uniqueId,
        )
        placeholders.putAll(server.properties.map {
            "%${it.key.uppercase().replace("-", "_")}%" to it.value
        })
        return placeholders
    }

    override fun buildRuntime(server: Server, repository: GroupRuntimeDirectory): GroupRuntime {
        var env = "default"
        if (ScreenCapabilities.isScreenAvailable())
            env = "screen"
        val runtime = GroupRuntime(env)
        repository.save("${server.group}.yml", runtime)
        return runtime
    }
}