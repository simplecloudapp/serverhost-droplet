package app.simplecloud.droplet.serverhost.shared.actions.impl.configurate

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ConfigurateActionData(
    val configurator: String,
    val dir: String,
    val replace: Boolean = true,
)
