package app.simplecloud.droplet.serverhost.runtime.config.environment

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DockerStartConfig(
    val exposedPort: Int = 25565,
    val envMappings: Map<String, String> = mapOf("forwarding-secret" to "FORWARDING_SECRET"),
    val health: DockerHealthConfig? = DockerHealthConfig(),
)
