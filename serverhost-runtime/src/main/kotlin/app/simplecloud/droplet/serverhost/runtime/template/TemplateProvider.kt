package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.host.ServerVersionLoader
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes
import app.simplecloud.droplet.serverhost.shared.docker.DockerClientInstance
import app.simplecloud.droplet.serverhost.shared.template.YamlTemplate
import app.simplecloud.droplet.serverhost.shared.template.YamlTemplateExecutor
import app.simplecloud.droplet.serverhost.shared.template.YamlTemplateLoader
import com.github.dockerjava.api.DockerClient
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory

class TemplateProvider(private val args: ServerHostStartCommand, private val actionProvider: ActionProvider) {

    private val logger = LogManager.getLogger(TemplateProvider::class.java)
    private var templates = listOf<YamlTemplate>()
    private val dir = Path.of(args.templateDefinitionPath.absolutePathString(), "definitions")

    fun load() {
        if (!dir.isDirectory()) {
            logger.error("Template directory is not a directory")
            return
        }
        val result = YamlTemplateLoader.load(dir, actionProvider.getLoadedActions())
        val errors = result.second
        errors.forEach { logger.error(it.message) }
        templates = result.first
        logger.info("Loaded ${templates.size} templates")
    }

    fun getLoadedTemplates(): List<YamlTemplate> {
        return templates
    }

    fun getLoadedTemplate(name: String): YamlTemplate? {
        return templates.find { it.name == name }
    }

    /**
     * @return the [YamlActionContext] for extended usage in internal functionality
     */
    fun execute(
        server: Server, serverDir: Path, template: YamlTemplate, on: YamlActionTriggerTypes
    ): YamlActionContext {
        val executor = YamlTemplateExecutor(actionProvider.getLoadedActions())
        val ctx = YamlActionContext()
        val placeholders = YamlActionPlaceholderContext()
        placeholders.setLibs(args.libsPath)
        placeholders.setTemplate(args.templatePath)
        placeholders.setRunning(args.runningServersPath)
        placeholders.set("forwarding-secret", args.forwardingSecret)
        placeholders.setServerDir(serverDir)
        placeholders.save(ctx)
        ctx.store("forwarding-secret", args.forwardingSecret)
        ctx.store("server", server)
        ctx.store("actions", actionProvider.getLoadedActions())
        ctx.store("docker-client", getDockerClient() ?: Any())
        ctx.store(
            "server-jar",
            ServerVersionLoader.getAndDownloadServerJar(server.properties["server-url"] ?: "").toPath()
        )
        ctx.store("configurator-jar", Paths.get("cache", "jars", "configurator.jar"))
        ctx.store("last-checksum", server.properties["last-checksum"] ?: "")
        ctx.store("libs", args.libsPath)
        executor.execute(template, on, ctx).forEach { exception ->
            logger.warn(exception.message ?: "Unknown error on template of group ${server.group}")
        }
        return ctx
    }

    private fun getDockerClient(): DockerClient? {
        try {
            return DockerClientInstance.new(args.dockerConfigPath)
        } catch (e: Exception) {
            logger.warn("Could not instantiate docker client: ${e.message}")
            return null
        }
    }
}