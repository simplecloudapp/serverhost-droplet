package app.simplecloud.droplet.serverhost.runtime.config.environment

import app.simplecloud.droplet.serverhost.runtime.config.YamlDirectoryRepository
import app.simplecloud.droplet.serverhost.runtime.config.environment.generators.DefaultDockerConfigGenerator
import app.simplecloud.droplet.serverhost.runtime.config.environment.generators.DefaultEnvironmentConfigGenerator
import app.simplecloud.droplet.serverhost.runtime.config.environment.generators.ScreenEnvironmentConfigGenerator

interface EnvironmentConfigGenerator {
    fun generate(): EnvironmentConfig
    fun getName(): String

    companion object {
        fun generateAll(repository: YamlDirectoryRepository<EnvironmentConfig>) {
            repository.save("screen.yml", ScreenEnvironmentConfigGenerator.generate(), false)
            repository.save("default.yml", DefaultEnvironmentConfigGenerator.generate(), false)
            repository.save("docker.yml", DefaultDockerConfigGenerator.generate(), false)
        }
    }
}