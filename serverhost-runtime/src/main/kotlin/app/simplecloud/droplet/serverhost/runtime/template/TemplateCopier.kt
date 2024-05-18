package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner

class TemplateCopier(
    private val args: ServerHostStartCommand,
) {

    fun copy(server: Server, runner: ServerRunner, actionType: TemplateActionType) {
        val path = "${server.properties.getOrDefault("template-id", server.group)}.yml"
        val template = Template.Config.load<Template>(path) ?: return

        when (actionType) {
            TemplateActionType.DEFAULT -> {
                template.destinations.forEach {
                    actionType.executor.execute(it, server, args, runner)
                }
            }

            TemplateActionType.RANDOM -> {
                val randomAction = template.randomDestinations.random()
                actionType.executor.execute(randomAction, server, args, runner)
            }

            TemplateActionType.SHUTDOWN -> template.shutdownDestinations.forEach {
                actionType.executor.execute(it, server, args, runner)
            }
        }
    }

}