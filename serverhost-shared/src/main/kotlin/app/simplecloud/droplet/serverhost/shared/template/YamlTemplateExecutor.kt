package app.simplecloud.droplet.serverhost.shared.template

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionGroupExecutor
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTypes

class YamlTemplateExecutor(
    private val loadedActions: Map<String, List<Pair<YamlActionTypes, Any>>>
) {

    fun execute(template: YamlTemplate, on: YamlActionTriggerTypes, context: YamlActionContext = YamlActionContext()) {
        val groupExecutor = YamlActionGroupExecutor(context, loadedActions)
        val toExecute = template.actionMap.getOrDefault(on, listOf())
        toExecute.forEach { action ->
            groupExecutor.execute(action)
        }
    }
}