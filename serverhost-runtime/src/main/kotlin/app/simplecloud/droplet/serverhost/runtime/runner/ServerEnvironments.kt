package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfigRepository
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.docker.DockerServerEnvironment
import app.simplecloud.droplet.serverhost.runtime.runner.process.ProcessServerEnvironment
import app.simplecloud.droplet.serverhost.runtime.template.TemplateProvider
import build.buf.gen.simplecloud.controller.v1.ControllerGroupServiceGrpcKt
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpcKt
import build.buf.gen.simplecloud.controller.v1.UpdateServerRequest
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager

class ServerEnvironments(
    templateProvider: TemplateProvider,
    serverHost: ServerHost,
    args: ServerHostStartCommand,
    private val controllerStub: ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub,
    groupStub: ControllerGroupServiceGrpcKt.ControllerGroupServiceCoroutineStub,
    metricsTracker: MetricsTracker,
    environmentsRepository: EnvironmentConfigRepository,
    runtimeRepository: GroupRuntimeDirectory,
) {

    private val process = ProcessServerEnvironment(
        templateProvider,
        serverHost,
        args,
        controllerStub,
        metricsTracker,
        environmentsRepository,
        runtimeRepository
    )
    private val docker = DockerServerEnvironment(
        serverHost,
        args,
        controllerStub,
        groupStub,
        metricsTracker,
        environmentsRepository,
        templateProvider,
        runtimeRepository
    )

    private val envs = listOf(
        docker,
        process,
    )

    private val logger = LogManager.getLogger(ServerEnvironments::class.java)

    /**
     * Gets the environment a server is running on or null if the server is not running in any environment
     */
    fun of(server: Server): ServerEnvironment? {
        return of(server.uniqueId)
    }

    /**
     * Gets the environment a server is running on or null if the server is not running in any environment
     */
    fun of(uniqueId: String): ServerEnvironment? {
        return envs.firstOrNull {
            it.getServer(uniqueId)?.let { server ->
                it.getEnvironment(server)
                    ?.let { env -> it.appliesFor(env) || it.appliesFor(server) }
            } ?: false
        }
    }

    fun getAll(): List<ServerEnvironment> {
        return envs
    }

    /**
     * Returns the initial environment used for the server
     */
    fun firstFor(server: Server): ServerEnvironment {
        return envs.firstOrNull {
            it.getEnvironment(server)?.let { env -> it.appliesFor(env) || it.appliesFor(server) } ?: false
        } ?: process
    }

    fun startServerStateChecker(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                envs.forEach { env ->
                    env.getServers().forEach {
                        var delete = false
                        var server = it
                        try {
                            val updated = env.updateServer(it)
                            if (updated == null) {
                                delete = true
                                env.stopServer(server)
                            } else {
                                server = updated
                                env.updateServerCache(updated.uniqueId, updated)
                            }
                            controllerStub.updateServer(
                                UpdateServerRequest.newBuilder()
                                    .setServer(server.toDefinition())
                                    .setDeleted(delete).build()
                            )
                        } catch (e: Exception) {
                            logger.error("An error occurred whilst updating the server:", e)
                        }
                    }
                }
                delay(5000L)
            }
        }
    }
}