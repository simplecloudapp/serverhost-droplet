package app.simplecloud.droplet.serverhost.shared.actions

data class YamlActionData(
    val type: YamlActionTypes,
    val data: Any,
    val async: Boolean = false
)
