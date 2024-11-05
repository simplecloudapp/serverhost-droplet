package app.simplecloud.droplet.serverhost.shared.actions

import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class YamlActionData(
    val type: YamlActionTypes,
    val data: ConfigSerializable,
)
