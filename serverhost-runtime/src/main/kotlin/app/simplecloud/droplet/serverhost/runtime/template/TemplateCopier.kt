package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner

class TemplateCopier(
    private val args: ServerHostStartCommand,
) {
    fun copy(server: Server, runner: ServerRunner, actionType: TemplateActionType) {
        val template = Template.Config.load<Template>("${server.group}.yml")
        when (actionType) {
            TemplateActionType.DEFAULT -> {
                template?.destinations?.forEach {
                    actionType.executor().execute(it, server, args, runner)
                }
            }

            TemplateActionType.RANDOM -> template?.randomDestinations?.forEach {
                actionType.executor().execute(it, server, args, runner)
            }

            TemplateActionType.SHUTDOWN -> template?.shutdownDestinations?.forEach {
                actionType.executor().execute(it, server, args, runner)
            }
        }
    }
}