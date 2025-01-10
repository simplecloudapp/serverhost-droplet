package app.simplecloud.serverhost.cli

import app.simplecloud.serverhost.configurator.Configurable

class EnvConfigurable(private val args: ConfiguratorStartCommand) : Configurable {
    override fun getPlaceholderMappings(): Map<String, Any> {
        val env = System.getenv().filter { it.key.startsWith(args.prefix) }
            .map { it.key.replaceFirst(args.prefix, "").lowercase().replace("_", "-") to it.value }.toMap()
            .toMutableMap()
        return env
    }

}