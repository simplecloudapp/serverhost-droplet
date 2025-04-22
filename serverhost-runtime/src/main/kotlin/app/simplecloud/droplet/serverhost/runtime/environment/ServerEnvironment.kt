package app.simplecloud.droplet.serverhost.runtime.environment

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfig
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfigRepository
import app.simplecloud.droplet.serverhost.runtime.template.TemplateProvider
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes
import app.simplecloud.droplet.serverhost.shared.hack.ServerPinger
import build.buf.gen.simplecloud.controller.v1.*
import kotlinx.coroutines.flow.Flow
import org.apache.logging.log4j.LogManager
import java.nio.file.Path

abstract class ServerEnvironment(
    protected open val runtimeRepository: GroupRuntimeDirectory,
    private val environmentRepository: EnvironmentConfigRepository
) {

    private val logger = LogManager.getLogger(ServerEnvironment::class.java)

    /**
     * Called on startup to build groups
     * @param [server] a fake server used in the build process (To make templates work correctly)
     * @param [context] The context of the method call
     */
    open fun build(server: Server, context: BuildContext = BuildContext.STARTUP) {

    }

    /**
     * Starts a [Server] and returns true if the server is successfully started
     */
    abstract suspend fun startServer(server: Server): Boolean

    /**
     * Stops a [Server] and returns true if the server is successfully stopped
     */
    abstract suspend fun stopServer(server: Server): Boolean

    /**
     * Updates a [Server] and returns the updated server or null if the server does not exist
     */
    abstract suspend fun updateServer(server: Server): Server?

    /**
     * Get a [Server] by unique id (returns null if the server is not running on this environment)
     */
    abstract fun getServer(uniqueId: String): Server?

    /**
     * Reattaches a [Server] and returns true if the server is successfully reattached
     */
    abstract fun reattachServer(server: Server): Boolean

    /**
     * Executes a command on a [Server] and returns true if execution is successful
     */
    abstract fun executeCommand(server: Server, command: String): Boolean

    /**
     * Returns a never ending stream for server logs
     */
    abstract fun streamLogs(server: Server): Flow<ServerHostStreamServerLogsResponse>

    /**
     * Precondition if the current environment is valid for a given [EnvironmentConfig]
     */
    abstract fun appliesFor(env: EnvironmentConfig): Boolean

    /**
     * Precondition if the current environment is valid for a given [Server]
     */
    open fun appliesFor(server: Server): Boolean {
        return false
    }

    /**
     * Returns all servers currently known to this environment.
     */
    abstract fun getServers(): List<Server>

    open fun getServerCache(): MutableMap<Server, *> {
        return mutableMapOf<Server, String>()
    }

    open fun executeTemplate(dir: Path, server: Server, on: YamlActionTriggerTypes, provider: TemplateProvider): YamlActionContext? {
        val template = provider.getLoadedTemplate(server.properties["template-id"] ?: "")
        if (template != null) {
            return provider.execute(server, dir, template, on)
        } else {
            if (!server.properties.containsKey("template-id"))
                logger.error("Group ${server.group} has no template defined!")
            else
                logger.error("Template ${server.properties["template-id"] ?: ""} of group ${server.group} was not found!")
        }
        return null
    }

    /**
     * Updates the cached server for this environment.
     */
    open fun updateServerCache(uniqueId: String, server: Server) {
        @Suppress("UNCHECKED_CAST")
        val cache = getServerCache() as MutableMap<Server, Any>
        val key = cache.keys.find { it.uniqueId == uniqueId }
        if (key == null) {
            logger.warn("Server ${server.group}-${server.numericalId} could not be updated in cache")
            return
        }
        val value = cache[key]!!
        cache.remove(key)
        cache[server] = value
    }

    fun getEnvironment(server: Server): EnvironmentConfig? {
        return environmentRepository.get(runtimeRepository.get(server.group))
    }

    protected suspend fun updateServer(server: Server, ping: ServerPinger.StatusResponse, controllerStub: ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub): Server {
        val controllerServer = controllerStub.getServerById(getServerByIdRequest {
            this.serverId = server.uniqueId
        })

        val copiedServer = Server.fromDefinition(controllerServer.copy {
            this.serverState =
                if (ping.description.text == "INGAME")
                    ServerState.INGAME
                else if (server.state == ServerState.STARTING)
                    ServerState.AVAILABLE
                else
                    server.state
            this.maxPlayers = ping.players.max.toLong()
            this.playerCount = ping.players.online.toLong()
            this.cloudProperties["motd"] = ping.description.text
        })
        return copiedServer
    }
}