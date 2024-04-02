package app.simplecloud.droplet.serverhost.runtime.template.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.runtime.template.TemplateAction
import app.simplecloud.droplet.serverhost.runtime.template.TemplateActionExecutor
import app.simplecloud.droplet.serverhost.runtime.template.TemplatePlaceholders
import org.apache.commons.io.FileUtils
import java.io.File

class ShutdownTemplateExecutor : TemplateActionExecutor {
    override fun execute(action: TemplateAction, server: Server): Boolean {
        try {
            val fromPath = TemplatePlaceholders.parsePath(action.copyFrom, "")
            val toPath = TemplatePlaceholders.parsePath(action.copyTo, TemplatePlaceholders.TEMPLATE_PATH)
            val to = File(TemplatePlaceholders.parse(toPath, server))
            val from = File(ServerRunner.getServerDir(server), TemplatePlaceholders.parse(fromPath, server))
            if (from.isDirectory)
                FileUtils.copyDirectory(from, to)
            else
                FileUtils.copyFile(from, to)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}