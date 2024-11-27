package app.simplecloud.droplet.serverhost.shared.template

data class YamlTemplate(
    val name: String,
    val actionMap: YamlTemplateActionsMap,
    val data: YamlTemplateData,
)