package app.simplecloud.droplet.serverhost.runtime.config.environment

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DockerHealthConfig(
    val testCommand: List<String> = listOf(
        "sh",
        "-c",
        "'if echo > /dev/tcp/localhost/%EXPOSED_PORT%; then exit 0; else exit 1; fi'"
    ),
    val interval: Long = 30000000000L,
    val retries: Int = 3,
    val timeout: Long = 10000000000L,
)