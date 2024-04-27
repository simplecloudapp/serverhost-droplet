package app.simplecloud.droplet.serverhost.runtime.template.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.runtime.template.TemplateAction
import app.simplecloud.droplet.serverhost.runtime.template.TemplateActionExecutor
import app.simplecloud.droplet.serverhost.runtime.template.TemplatePlaceholders
import org.apache.commons.io.FileUtils
import java.io.File

class ShutdownTemplateExecutor : TemplateActionExecutor {
    override fun execute(action: TemplateAction, server: Server, args: ServerHostStartCommand, runner: ServerRunner): Boolean {
        try {
            val parsedTo = TemplatePlaceholders.parse(action.copyTo, server)
            val toPath = TemplatePlaceholders.parsePath(parsedTo, args.templatePath)
            val parsedFrom = TemplatePlaceholders.parse(action.copyFrom, server)
            val fromPath = TemplatePlaceholders.parsePath(parsedFrom, runner.getServerDir(server).toPath())
            val from = fromPath.toFile()
            val to = toPath.toFile()
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