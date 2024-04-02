package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server

class TemplateCopier {
    fun copy(server: Server, actionType: TemplateActionType) {
        val template = Template.Config.load<Template>("${server.group}.yml")
        when (actionType) {
            TemplateActionType.DEFAULT -> {
                template?.destinations?.forEach {
                    actionType.executor().execute(it, server)
                }
            }

            TemplateActionType.RANDOM -> template?.randomDestinations?.forEach {
                actionType.executor().execute(it, server)
            }

            TemplateActionType.SHUTDOWN -> template?.shutdownDestinations?.forEach {
                actionType.executor().execute(it, server)
            }
        }
    }
}