package app.simplecloud.droplet.serverhost.shared.actions.impl

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ConfigurateActionData(
    val configurator: String,
    val dir: String,
)
