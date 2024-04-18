package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner

interface TemplateActionExecutor {
    fun execute(action: TemplateAction, server: Server, args: ServerHostStartCommand, runner: ServerRunner): Boolean
}