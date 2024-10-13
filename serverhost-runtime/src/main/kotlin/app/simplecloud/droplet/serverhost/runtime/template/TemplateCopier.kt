package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import org.apache.logging.log4j.LogManager
import java.nio.file.Path

class TemplateCopier(
    private val args: ServerHostStartCommand,
) {


    private val loadedTemplates = mutableMapOf<String, Template>()
    private val logger = LogManager.getLogger(TemplateCopier::class)

    fun copy(server: Server, runner: ServerRunner, actionType: TemplateActionType) {
        val templateName = server.properties.getOrDefault("template-id", server.group)
//        val template = loadTemplateWithExtends("templates", templateName) ?: return

        var templateId = server.properties["template-id"]
        if (templateId == null) {
            templateId = server.group
            Template.Config.load<Template>(templateId)
            loadTemplates()
        }
        val template = loadTemplate(templateId)

        when (actionType) {
            TemplateActionType.DEFAULT -> {
                template?.destinations?.forEach {
                    actionType.executor.execute(it, server, args, runner)
                }
            }

            TemplateActionType.RANDOM -> {
                if (template?.randomDestinations?.isEmpty() != false) {
                    return
                }

                val randomAction = template.randomDestinations.random()
                actionType.executor.execute(randomAction, server, args, runner)
            }

            TemplateActionType.SHUTDOWN -> template?.shutdownDestinations?.forEach {
                actionType.executor.execute(it, server, args, runner)
            }
        }
    }

    private fun loadTemplate(
        id: String,
        loaded: MutableList<String> = mutableListOf(),
        current: Template? = null
    ): Template? {
        if (loaded.contains(id)) {
            logger.warn("Template $id could not be loaded correctly. Did you make your template recursive by accident?")
            return current
        }
        val template = loadedTemplates.getOrDefault(id, null) ?: return current
        val existing = current ?: template
        if (existing != template) {
            existing.destinations.addAll(template.destinations)
            existing.randomDestinations.addAll(template.randomDestinations)
            existing.shutdownDestinations.addAll(template.shutdownDestinations)
        }
        loaded.add(id)
        if (template.extends != null) {
            return loadTemplate(template.extends, loaded, existing)
        }
        return existing
    }

    fun loadTemplates() {
        val path = Path.of("templates")
        loadedTemplates.clear()
        loadedTemplates.putAll(Template.Config.loadAll<Template>(path))
    }

//    private fun loadTemplate(
//        directory: String,
//        templatePath: String,
//        templates: MutableMap<String, Template>
//    ): Template? {
//        if (templates.containsKey(templatePath)) {
//            return templates[templatePath]!!
//        }
//
//        val file = File(directory).walkTopDown()
//            .find { it.isFile && it.extension == "yml" && it.path.endsWith("${templatePath}.yml") }
//            ?: throw FileNotFoundException("Template file not found: $templatePath")
//
//        if (!file.exists()) {
//            println("Template file not found: $templatePath")
//            return null
//        }
////        return null
//
//        val template = Template.Config.load<Template>(file.path) ?: return null
//        println("loading template ${file.path} ${file.absolutePath}")
//        templates[templatePath] = template
//
//        if (template.extends != null) {
//            val parentTemplate = loadTemplate(directory, template.extends, templates) ?: return null
//            template.merge(parentTemplate)
//        }
//
//        println("loading template2 ${file.path} ${file.absolutePath}")
//
//        return template
//    }
//
//    private fun loadTemplateWithExtends(directory: String, templateName: String): Template? {
//        val templates = loadTemplates(directory)
//        if (!templates.containsKey(templateName)) {
//            return null
//        }
//        val template = templates[templateName] ?: return null
//
//        val mergedTemplate =  getAndMerge(template, templates)
//        println(mergedTemplate)
//        return mergedTemplate
////        return loadTemplate(directory, templatePath, templates)
//    }
//
//    private fun getAndMerge(template: Template, templates: Map<String, Template>): Template? {
//        val extend = template.extends ?: return null
//        val extendedTemplate = templates[extend]?: return null
//
//        extendedTemplate.merge(template)
//        return getAndMerge(extendedTemplate, templates)
//    }
//
//    private fun loadTemplates(directory: String): Map<String, Template> {
//        return File(directory).walkTopDown()
//            .filter { it.isFile && it.extension == "yml" }
//            .mapNotNull {
//                println("loading template ${it.path}")
//                val template = Template.Config.load<Template>(it.path) ?: return@mapNotNull null
//                it.nameWithoutExtension to template
//            }
//            .toMap()
//
//    }

}