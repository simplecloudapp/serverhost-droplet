package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfigRepository
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.template.TemplateProvider
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpcKt

class ServerEnvironments(
    templateProvider: TemplateProvider,
    serverHost: ServerHost,
    args: ServerHostStartCommand,
    controllerStub: ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub,
    metricsTracker: MetricsTracker,
    environmentsRepository: EnvironmentConfigRepository,
    runtimeRepository: GroupRuntimeDirectory,
) {

    private val default = DefaultServerEnvironment(
        templateProvider,
        serverHost,
        args,
        controllerStub,
        metricsTracker,
        environmentsRepository,
        runtimeRepository
    )

    private val envs = listOf(
        default
    )

    fun of(server: Server): ServerEnvironment? {
        return of(server.uniqueId)
    }

    fun of(uniqueId: String): ServerEnvironment? {
        return envs.firstOrNull {
            it.getServer(uniqueId)?.let { server ->
                it.getEnvironment(server)
                    ?.let { env -> it.appliesFor(env) }
            } ?: false
        }
    }

    fun getAll(): List<ServerEnvironment> {
        return envs
    }

    fun firstFor(server: Server): ServerEnvironment {
        return envs.firstOrNull { it.getEnvironment(server)?.let { env -> it.appliesFor(env) } ?: false } ?: default
    }
}