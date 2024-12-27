package app.simplecloud.droplet.serverhost.runtime.config.environment

import app.simplecloud.droplet.serverhost.runtime.config.YamlDirectoryRepository
import app.simplecloud.droplet.serverhost.runtime.config.environment.generators.DefaultEnvironmentConfigGenerator
import app.simplecloud.droplet.serverhost.runtime.config.environment.generators.ScreenEnvironmentConfigGenerator
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand

interface EnvironmentConfigGenerator {
    fun generate(args: ServerHostStartCommand): EnvironmentConfig
    fun getName(): String

    companion object {


        fun generateAll(args: ServerHostStartCommand, repository: YamlDirectoryRepository<EnvironmentConfig>) {
            repository.save("screen.yml", ScreenEnvironmentConfigGenerator.generate(args), false)
            repository.save("default.yml", DefaultEnvironmentConfigGenerator.generate(args), false)
        }
    }
}