package app.simplecloud.droplet.serverhost.runtime.config.environment

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DockerHealthConfig(
    val testCommand: List<String> = listOf(
        "CMD-SHELL",
        "busybox netstat -tln | grep -q ':%EXPOSED_PORT% ' || exit 1",
    ),
    val interval: Long = 5000000000L,
    val retries: Int = 3,
    val timeout: Long = 3000000000L,
)