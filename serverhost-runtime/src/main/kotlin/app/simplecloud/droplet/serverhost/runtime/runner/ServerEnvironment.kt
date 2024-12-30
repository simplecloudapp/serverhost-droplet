package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfig
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfigRepository
import build.buf.gen.simplecloud.controller.v1.ServerHostStreamServerLogsResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

abstract class ServerEnvironment(
    protected open val runtimeRepository: GroupRuntimeDirectory,
    private val environmentRepository: EnvironmentConfigRepository
) {
    /**
     * Starts a [Server] and returns true if the server is successfully started
     */
    abstract suspend fun startServer(server: Server): Boolean

    /**
     * Stops a [Server] and returns true if the server is successfully stopped
     */
    abstract suspend fun stopServer(server: Server): Boolean

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
     * Starts the server update checker. This will stop offline servers and update servers by the ping response of the server.
     */
    abstract fun startServerStateChecker(): Job

    fun getEnvironment(server: Server): EnvironmentConfig? {
        return environmentRepository.get(runtimeRepository.get(server.group))
    }
}