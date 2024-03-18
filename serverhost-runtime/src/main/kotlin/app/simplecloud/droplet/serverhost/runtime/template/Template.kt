package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.droplet.serverhost.runtime.config.YamlGroupConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class Template(
    val destinations: List<TemplateAction>,
    val randomDestinations: List<TemplateAction>,
    val shutdownDestinations: List<TemplateAction>
) {
    companion object {
        val Config = YamlGroupConfig("templates")
    }
}
