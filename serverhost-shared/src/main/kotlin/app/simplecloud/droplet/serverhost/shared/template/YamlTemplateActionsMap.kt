package app.simplecloud.droplet.serverhost.shared.template

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes

interface YamlTemplateActionsMap : Map<YamlActionTriggerTypes, List<String>>

class InferredYamlTemplateActionsMap(
    private val backingMap: Map<YamlActionTriggerTypes, List<String>>
) : YamlTemplateActionsMap, Map<YamlActionTriggerTypes, List<String>> by backingMap