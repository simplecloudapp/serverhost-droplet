package app.simplecloud.droplet.serverhost.shared.actions

// This class is able to execute actions parsed by the YamlActionLoader
class YamlActionGroupExecutor(private val loadedActions: Map<String, List<Pair<YamlActionTypes, Any>>>) {
    fun execute(ref: String): Boolean {
        val loadedActionGroup = loadedActions[ref] ?: return false
        try {
            val context = YamlActionContext()
            for (actionData in loadedActionGroup) {
                val type = actionData.first
                exec(actionData.second, context, type.action)
            }
            return true
        }catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun exec(data: Any, ctx: YamlActionContext, action: YamlAction<*>) {
        action.javaClass.getMethod("exec", YamlActionContext::class.java, Any::class.java).invoke(action, ctx, data)
    }
}