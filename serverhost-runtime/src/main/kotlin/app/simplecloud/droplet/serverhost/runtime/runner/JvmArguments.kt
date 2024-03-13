package app.simplecloud.droplet.serverhost.runtime.runner

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class JvmArguments(
    val executable: String?,
    val options: List<String>?,
    val arguments: List<String>?,
)

/**
 *
 * example.yml
 *
 * executable: C:/test/java/bin/java
 * options:
 *      - '-Dcom.minecraft.eula=true'
 * arguments:
 *      - 'nogui'
 */