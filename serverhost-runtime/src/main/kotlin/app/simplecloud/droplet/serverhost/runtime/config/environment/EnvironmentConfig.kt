package app.simplecloud.droplet.serverhost.runtime.config.environment

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class EnvironmentConfig(
    val enabled: Boolean = true,
    val isScreen: Boolean = false,
    val useScreenStop: Boolean = false,
    val isDocker: Boolean = false,
    val buildPolicy: BuildPolicy = BuildPolicy.NEVER,
    val imagePullPolicy: ImagePullPolicy = ImagePullPolicy.NEVER,
    val name: String = "",
    val start: EnvironmentStartConfig? = null,
    val stop: EnvironmentStopConfig? = null,
    val version: String = "1",
) {
    fun getExecutable(): String? {
        if (isDocker) return null
        var index = 0
        if (isScreen) {
            while (index < (start?.command?.size ?: 0)) {
                val current = start?.command?.get(index) ?: ""
                if (current == "-dmS" || current == "-S") {
                    return start?.command?.get(index + 2) ?: ""
                }
                index++
            }
            return null
        }
        return start?.command?.get(0)
    }

    fun getRealExecutable(): String? {
        return start?.command?.get(0)
    }

}