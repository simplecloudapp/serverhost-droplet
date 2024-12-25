package app.simplecloud.droplet.serverhost.shared.template

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionGroupExecutor
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTypes

open class YamlTemplateExecutor(
    private val loadedActions: Map<String, List<Pair<YamlActionTypes, Any>>>
) {

    open fun execute(template: YamlTemplate, on: YamlActionTriggerTypes, context: YamlActionContext = YamlActionContext()): List<Exception> {
        val groupExecutor = YamlActionGroupExecutor(context, loadedActions)
        val toExecute = template.actionMap.getOrDefault(on, listOf())
        val errors = mutableListOf<Exception>()
        toExecute.forEach { action ->
            try {
                errors.addAll(groupExecutor.execute(action))
            }catch (e: Exception) {
                errors.add(e)
            }
        }
        return errors
    }
}