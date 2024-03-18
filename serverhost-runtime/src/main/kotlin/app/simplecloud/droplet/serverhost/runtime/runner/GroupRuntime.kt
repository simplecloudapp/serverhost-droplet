package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.droplet.serverhost.runtime.config.YamlGroupConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class GroupRuntime(val jvm: JvmArguments? = null, val ignore: Boolean? = false, val parentDir: String?) {
    companion object {
        val Config = YamlGroupConfig("options")
    }
}
