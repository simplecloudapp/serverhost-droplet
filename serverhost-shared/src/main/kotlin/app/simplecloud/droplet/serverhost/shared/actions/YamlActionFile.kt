package app.simplecloud.droplet.serverhost.shared.actions

data class YamlActionFile(
    val fileName: String,
    val groups: List<YamlActionGroup>
)