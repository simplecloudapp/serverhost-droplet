package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand

interface EnvironmentBuilder<T> {
    fun buildEnv(server: Server): T
    fun createRuntimePlaceholders(server: Server): Map<String, String>
    fun buildRuntime(server: Server, repository: GroupRuntimeDirectory): GroupRuntime
    fun addAllWithPlaceholders(list: MutableList<String>, placeholders: Map<String, String>) {
        val result = list.map {
            var returned = it
            placeholders.keys.map { placeholder ->
                returned = returned.replace(placeholder, placeholders.getOrDefault(placeholder, ""))
            }
            return@map returned
        }
        list.clear()
        list.addAll(result)
    }

    fun getDefaultEnv(serverHost: ServerHost, args: ServerHostStartCommand): Map<String, String> {
        return mapOf(
            "HOST_IP" to serverHost.host,
            "HOST_PORT" to serverHost.port.toString(),
            "CONTROLLER_HOST" to args.grpcHost,
            "CONTROLLER_PORT" to args.grpcPort.toString(),
            "CONTROLLER_SECRET" to args.authSecret,
            "CONTROLLER_PUBSUB_HOST" to args.pubSubGrpcHost,
            "CONTROLLER_PUBSUB_PORT" to args.pubSubGrpcPort.toString(),
        )
    }

    companion object {
        fun buildRuntime(server: Server, repository: GroupRuntimeDirectory, env: String): GroupRuntime {
            val runtime = GroupRuntime(env)
            repository.save("${server.group}.yml", runtime)
            return runtime
        }
    }
}