package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server

interface TemplateActionExecutor {
    fun execute(action: TemplateAction, server: Server): Boolean
}