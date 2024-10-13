package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.droplet.serverhost.runtime.config.YamlGroupConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class Template(
    val extends: String?,
    val destinations: MutableList<TemplateAction>,
    val randomDestinations: MutableList<TemplateAction>,
    val shutdownDestinations: MutableList<TemplateAction>
) {
    companion object {
        val Config = YamlGroupConfig("templates")
    }

    fun merge(extending: Template) {
        extending.destinations.addAll(this.destinations)
        extending.randomDestinations.addAll(this.randomDestinations)
        extending.shutdownDestinations.addAll(this.shutdownDestinations)
    }
}
