package app.simplecloud.droplet.serverhost.shared.actions

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class ParallelActionGroupExecutor(
    context: YamlActionContext,
    loadedActions: Map<String, List<Pair<YamlActionTypes, Any>>>
) : YamlActionGroupExecutor(context, loadedActions) {

    override fun execute(ref: String): List<Exception> {
        val loadedActionGroup = loadedActions[ref] ?: return listOf(NullPointerException("$ref does not exist"))
        val exceptions = ConcurrentLinkedQueue<Exception>()

        try {
            runBlocking {
                val asyncActions = mutableListOf<Pair<YamlActionTypes, Any>>()

                for (actionData in loadedActionGroup) {
                    when {
                        // Group actions marked as async for parallel execution
                        actionData.second is YamlActionData && (actionData.second as YamlActionData).async -> {
                            asyncActions.add(actionData)
                        }

                        asyncActions.isNotEmpty() -> {
                            executeActionsInParallel(asyncActions, exceptions)
                            asyncActions.clear()
                            executeSingleAction(actionData, exceptions)
                        }

                        else -> {
                            executeSingleAction(actionData, exceptions)
                        }
                    }
                }

                // Execute any remaining async actions
                if (asyncActions.isNotEmpty()) {
                    executeActionsInParallel(asyncActions, exceptions)
                }
            }

            return exceptions.toList()
        } catch (e: Exception) {
            exceptions.add(e)
        }
        return exceptions.toList()
    }

    private suspend fun executeActionsInParallel(
        actions: List<Pair<YamlActionTypes, Any>>,
        exceptions: ConcurrentLinkedQueue<Exception>
    ) {
        coroutineScope {
            actions.map { actionData ->
                async(Dispatchers.IO) {
                    try {
                        val yamlData = actionData.second as YamlActionData
                        exec(yamlData.data, context, actionData.first.action)
                    } catch (e: Exception) {
                        exceptions.add(
                            YamlActionException(
                                "Parallel action failed: ${e.javaClass.simpleName}: ${e.cause?.message ?: "Unknown"}",
                                e
                            )
                        )
                    }
                }
            }.awaitAll()
        }
    }

    private fun executeSingleAction(
        actionData: Pair<YamlActionTypes, Any>,
        exceptions: ConcurrentLinkedQueue<Exception>
    ) {
        try {
            val yamlData = actionData.second as YamlActionData
            exec(yamlData.data, context, actionData.first.action)
        } catch (e: Exception) {
            exceptions.add(
                YamlActionException(
                    "Action failed: ${e.javaClass.simpleName}: ${e.cause?.message ?: "Unknown"}",
                    e
                )
            )
        }
    }
}
