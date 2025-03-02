package app.simplecloud.droplet.serverhost.shared.actions

// This class is able to execute actions parsed by the YamlActionLoader
open class YamlActionGroupExecutor(protected val context: YamlActionContext, protected val loadedActions: Map<String, List<Pair<YamlActionTypes, Any>>>) {
    open fun execute(ref: String): List<Exception> {
        val loadedActionGroup = loadedActions[ref] ?: return listOf(NullPointerException("$ref does not exist"))
        val exceptions = mutableListOf<Exception>()
        try {
            for (actionData in loadedActionGroup) {
                val type = actionData.first
                try {
                    exec(actionData.second, context, type.action)
                }catch (e: Exception) {
                    exceptions.add(YamlActionException("action $ref failed: ${e.javaClass.simpleName}: ${e.cause?.message ?: "Unknown"}", e))
                }
            }
            return exceptions
        }catch (e: Exception) {
            exceptions.add(e)
        }
        return exceptions
    }

    protected open fun exec(data: Any, ctx: YamlActionContext, action: YamlAction<*>) {
        val actualData = if (data is YamlActionData) data.data else data
        action.javaClass.getMethod("exec", YamlActionContext::class.java, Any::class.java).invoke(action, ctx, actualData)
    }
}

class YamlActionException(message: String, other: Throwable): RuntimeException(message, other)
