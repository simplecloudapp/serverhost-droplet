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
    abstract suspend fun startServer(server: Server): Boolean
    abstract suspend fun stopServer(server: Server): Boolean
    abstract fun getServer(uniqueId: String): Server?
    abstract fun reattachServer(server: Server): Boolean
    abstract fun executeCommand(server: Server, command: String): Boolean
    abstract fun streamLogs(server: Server): Flow<ServerHostStreamServerLogsResponse>
    abstract fun appliesFor(env: EnvironmentConfig): Boolean
    abstract fun startServerStateChecker(): Job

    fun getEnvironment(server: Server): EnvironmentConfig? {
        return environmentRepository.get(runtimeRepository.get(server.group))
    }
}