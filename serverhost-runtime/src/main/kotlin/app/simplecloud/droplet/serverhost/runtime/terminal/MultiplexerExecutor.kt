package app.simplecloud.droplet.serverhost.runtime.terminal

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.runner.ServerEnvironment

open class MultiplexerExecutor(server: Server, env: ServerEnvironment) {
    protected val config = env.getEnvironment(server)

    private val placeholders = createRuntimePlaceholders(server)

    fun executeCommand(command: String): Boolean {
        if (config == null || !config.isScreen || config.update?.command == null) return false
        return execute(*replaceAndConvert(config.update.command), "stuff", "$command\\n")
    }

    fun executeCommand(vararg command: String): Boolean {
        return executeCommand(command.joinToString(" "))
    }

    protected fun execute(vararg command: String): Boolean {
        try {
            Runtime.getRuntime().exec(command)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun replaceAndConvert(list: List<String>): Array<String> {
        return list.map {
            var result = it; placeholders.forEach { placeholder ->
            result = result.replace(placeholder.key, placeholder.value)
        }; return@map result
        }.toTypedArray()
    }

    private fun createRuntimePlaceholders(server: Server): MutableMap<String, String> {
        val placeholders = mutableMapOf(
            "%MIN_MEMORY%" to server.minMemory.toString(),
            "%MAX_MEMORY%" to server.maxMemory.toString(),
            "%SESSION_NAME%" to "${server.group}-${server.numericalId}-${server.uniqueId.substring(0, 6)}",
            "%NUMERICAL_ID%" to server.numericalId.toString(),
            "%GROUP%" to server.group,
            "%UNIQUE_ID%" to server.uniqueId,
        )
        placeholders.putAll(server.properties.map {
            "%${it.key.uppercase().replace("-", "_")}%" to it.value
        })
        return placeholders
    }
}