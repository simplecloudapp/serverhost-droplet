package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import java.io.File
import java.io.FileNotFoundException

class TemplateCopier(
    private val args: ServerHostStartCommand,
) {

    fun copy(server: Server, runner: ServerRunner, actionType: TemplateActionType) {
        val templateName = server.properties.getOrDefault("template-id", server.group)
        val template = loadTemplateWithExtends("templates", templateName) ?: return

        when (actionType) {
            TemplateActionType.DEFAULT -> {
                template.destinations.forEach {
                    actionType.executor.execute(it, server, args, runner)
                }
            }

            TemplateActionType.RANDOM -> {
                if (template.randomDestinations.isEmpty()) {
                    return
                }

                val randomAction = template.randomDestinations.random()
                actionType.executor.execute(randomAction, server, args, runner)
            }

            TemplateActionType.SHUTDOWN -> template.shutdownDestinations.forEach {
                actionType.executor.execute(it, server, args, runner)
            }
        }
    }

    private fun loadTemplate(directory: String, templatePath: String, templates: MutableMap<String, Template>): Template? {
        if (templates.containsKey(templatePath)) {
            return templates[templatePath]!!
        }

        val file = File(directory).walkTopDown().find { it.isFile && it.extension == "yml" && it.path.endsWith("${templatePath}.yml") }
            ?: throw FileNotFoundException("Template file not found: $templatePath")

        val template = Template.Config.load<Template>(file.path)?: return null
        templates[templatePath] = template

        if (template.extends != null) {
            val parentTemplate = loadTemplate(directory, template.extends, templates)?: return null
            template.merge(parentTemplate)
        }

        return template
    }

    private fun loadTemplateWithExtends(directory: String, templatePath: String): Template? {
        val templates = mutableMapOf<String, Template>()
        return loadTemplate(directory, templatePath, templates)
    }

}