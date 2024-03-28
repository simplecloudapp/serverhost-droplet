package app.simplecloud.droplet.serverhost.runtime.template.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunnerPlaceholders
import app.simplecloud.droplet.serverhost.runtime.template.TemplateAction
import app.simplecloud.droplet.serverhost.runtime.template.TemplateActionExecutor
import app.simplecloud.droplet.serverhost.runtime.template.TemplatePlaceholders
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

class RandomTemplateExecutor : TemplateActionExecutor {
    override fun execute(action: TemplateAction, server: Server): Boolean {
        try {
            val fromPath = TemplatePlaceholders.parsePath(action.copyFrom, TemplatePlaceholders.TEMPLATE_PATH)
            val toPath = TemplatePlaceholders.parsePath(action.copyTo, "")
            val fromDir = File(TemplatePlaceholders.parse(fromPath, server))
            val childDirs = fromDir.listFiles() ?: return false
            val randomChild = childDirs[Random(childDirs.size).nextInt()]
            val to = File(ServerRunner.getServerDir(server), TemplatePlaceholders.parse(toPath, server))
            Files.copy(randomChild.toPath(), to.toPath())
            return true
        } catch (e: Exception) {
            return false
        }
    }
}