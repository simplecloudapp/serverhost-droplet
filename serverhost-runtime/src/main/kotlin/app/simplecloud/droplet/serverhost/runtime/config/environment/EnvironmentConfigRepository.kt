package app.simplecloud.droplet.serverhost.runtime.config.environment

import app.simplecloud.droplet.serverhost.runtime.config.YamlDirectoryRepository
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntime

class EnvironmentConfigRepository(args: ServerHostStartCommand) :
    YamlDirectoryRepository<EnvironmentConfig>(args.environmentsPath, EnvironmentConfig::class.java) {
    init {
        EnvironmentConfigGenerator.generateAll(args, this)
        load()
    }

    fun get(name: String): EnvironmentConfig? {
        return entities.values.find { it.name == name }
    }

    fun get(runtime: GroupRuntime?): EnvironmentConfig? {
        return runtime?.environment?.let { get(it) }
    }
}