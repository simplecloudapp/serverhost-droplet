package app.simplecloud.droplet.serverhost.runtime.template.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.runtime.template.TemplateAction
import app.simplecloud.droplet.serverhost.runtime.template.TemplateActionExecutor
import app.simplecloud.droplet.serverhost.runtime.template.TemplatePlaceholders
import org.apache.commons.io.FileUtils
import kotlin.random.Random

class RandomTemplateExecutor : TemplateActionExecutor {
    override fun execute(
        action: TemplateAction,
        server: Server,
        args: ServerHostStartCommand,
        runner: ServerRunner
    ): Boolean {
        try {
            val parsedFrom = TemplatePlaceholders.parse(action.copyFrom, server)
            val fromPath = TemplatePlaceholders.parsePath(parsedFrom, args.templatePath)
            val parsedTo = TemplatePlaceholders.parse(action.copyTo, server)
            val toPath = TemplatePlaceholders.parsePath(parsedTo, runner.getServerDir(server).toPath())
            val fromDir = fromPath.toFile()
            val childDirs = fromDir.listFiles() ?: return false
            val randomChild = childDirs[Random(childDirs.size).nextInt()]
            val to = toPath.toFile()
            if (randomChild.isDirectory)
                FileUtils.copyDirectory(randomChild, to)
            else
                FileUtils.copyFile(randomChild, to)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}