package app.simplecloud.droplet.serverhost.runtime.config.environment

import app.simplecloud.droplet.serverhost.runtime.config.YamlDirectoryRepository
import app.simplecloud.droplet.serverhost.runtime.environment.GroupRuntime
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import org.apache.logging.log4j.LogManager

class EnvironmentConfigRepository(args: ServerHostStartCommand) :
    YamlDirectoryRepository<EnvironmentConfig>(args.environmentsPath, EnvironmentConfig::class.java) {
    private val logger = LogManager.getLogger(EnvironmentConfigRepository::class.java)

    init {
        try {
            EnvironmentConfigGenerator.generateAll(this)
            load()
        } catch (e: Exception) {
            logger.error(e)
        }
    }

    fun get(name: String): EnvironmentConfig? {
        return entities.values.find { it.name == name }
    }

    fun get(runtime: GroupRuntime?): EnvironmentConfig? {
        return runtime?.environment?.let { get(it) }
    }
}