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
        var templateId = server.properties["template-id"]
        if(templateId == null) {
            templateId = server.group
            Template.Config.load<Template>(templateId)
            loadTemplates()
        }
        val template = loadTemplate(templateId)
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

    private fun loadTemplate(id: String, loaded: MutableList<String> = mutableListOf(), current: Template? = null): Template? {
        if(loaded.contains(id)) {
            logger.warn("Template $id could not be loaded correctly. Did you make your template recursive by accident?")
            return current
        }
        val template = loadedTemplates.getOrDefault(id, null) ?: return current
        val existing = current ?: template
        if(existing != template) {
            existing.destinations.addAll(template.destinations)
            existing.randomDestinations.addAll(template.randomDestinations)
            existing.shutdownDestinations.addAll(template.shutdownDestinations)
        }
        loaded.add(id)
        if(template.extends != null) {
            return loadTemplate(template.extends, loaded, existing)
        }
        return existing
    }

    fun loadTemplates() {
        val path = Path.of("templates")
        loadedTemplates.clear()
        loadedTemplates.putAll(Template.Config.loadAll<Template>(path))
    }
}