package app.simplecloud.droplet.serverhost.shared.actions.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.configurator.ServerConfigurable
import app.simplecloud.serverhost.configurator.ConfiguratorExecutor
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object ConfigurateAction : YamlAction<ConfigurateActionData> {

    private val configurator = ConfiguratorExecutor()

    override fun exec(ctx: YamlActionContext, data: ConfigurateActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholder context is required but was not found")
        val server =
            ctx.retrieve<Server>("server") ?: throw NullPointerException("server context is required but was not found")
        val forwardingSecret =
            ctx.retrieve<String>("forwarding-secret")
                ?: throw NullPointerException("forwarding-secret context is required but was not found")
        val dest = Paths.get(placeholders.parse(data.dir))
        if (!dest.exists() || !dest.isDirectory()) {
            throw NullPointerException("destination dir does not exist ($dest)")
        }
        val usedConfigurator = placeholders.parse(data.configurator)
        if (!configurator.configurate(
                ServerConfigurable(server),
                usedConfigurator,
                dest.toFile(),
                forwardingSecret,
                data.replace
            )
        ) throw Exception("Configurator $usedConfigurator failed.")
    }

    override fun getDataType(): Class<ConfigurateActionData> {
        return ConfigurateActionData::class.java
    }
}