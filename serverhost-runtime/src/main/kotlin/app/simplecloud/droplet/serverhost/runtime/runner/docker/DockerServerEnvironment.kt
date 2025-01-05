package app.simplecloud.droplet.serverhost.runtime.runner.docker

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.api.time.ProtobufTimestamp
import app.simplecloud.droplet.serverhost.runtime.config.environment.DockerStartConfig
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfig
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfigRepository
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntimeDirectory
import app.simplecloud.droplet.serverhost.runtime.runner.MetricsTracker
import app.simplecloud.droplet.serverhost.runtime.runner.ServerEnvironment
import app.simplecloud.droplet.serverhost.shared.docker.DockerClientInstance
import app.simplecloud.droplet.serverhost.shared.hack.PortProcessHandle
import app.simplecloud.droplet.serverhost.shared.hack.ServerPinger
import build.buf.gen.simplecloud.controller.v1.*
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.apache.logging.log4j.LogManager
import java.net.InetSocketAddress
import java.time.LocalDateTime

class DockerServerEnvironment(
    private val serverHost: ServerHost,
    private val args: ServerHostStartCommand,
    private val controllerStub: ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub,
    private val metricsTracker: MetricsTracker,
    environmentsRepository: EnvironmentConfigRepository,
    override val runtimeRepository: GroupRuntimeDirectory,
) : ServerEnvironment(runtimeRepository, environmentsRepository) {

    private lateinit var dockerClient: DockerClient
    private val logger = LogManager.getLogger(DockerServerEnvironment::class.java)

    private val serverToContainer = mutableMapOf<Server, String>()

    private fun containsServer(server: Server): Boolean {
        return serverToContainer.any { it.key.uniqueId == server.uniqueId }
    }

    override fun updateServerCache(uniqueId: String, server: Server) {
        val key = serverToContainer.keys.find { it.uniqueId == uniqueId }
        if (key == null) {
            logger.warn("Server ${server.group}-${server.numericalId} could not be updated in cache")
            return
        }
        val value = serverToContainer[key]!!
        serverToContainer.remove(key)
        serverToContainer[server] = value
    }

    /**
     * The reason this method exists is so no errors are thrown if docker is not installed until the environment is used
     */
    private fun getClientSafe(): DockerClient? {
        if (this::dockerClient.isInitialized) {
            return dockerClient
        }
        try {
            dockerClient = DockerClientInstance.new(args.dockerConfigPath)
            return dockerClient
        } catch (e: Exception) {
            logger.error("Could not instantiate docker client", e)
            return null
        }
    }

    override suspend fun startServer(server: Server): Boolean {
        val client = getClientSafe() ?: return false
        if (!server.properties.containsKey("docker-image") || !server.properties.containsKey("docker-tag")) {
            logger.error("Can not start a server when no docker image is specified")
            return false
        }
        val image = server.properties["docker-image"]!!
        val tag = server.properties["docker-tag"]!!
        if (!DockerUtils.imageExists(client, image, tag)) {
            logger.error("Provided image is not present")
            return false
        }
        val config = getEnvironment(server)?.start?.docker ?: DockerStartConfig()
        val env = EnvBuilder(
            config, args, mapOf(
                "HOST_IP" to serverHost.host,
                "HOST_PORT" to serverHost.port.toString(),
                "CONTROLLER_HOST" to this.args.grpcHost,
                "CONTROLLER_PORT" to this.args.grpcPort.toString(),
                "CONTROLLER_SECRET" to this.args.authSecret,
                "CONTROLLER_PUBSUB_HOST" to this.args.pubSubGrpcHost,
                "CONTROLLER_PUBSUB_PORT" to this.args.pubSubGrpcPort.toString(),
            )
        ).build(server)
        val containerId: String
        try {
            val container = client.createContainerCmd("$image:$tag")
                .withName("${server.group}-${server.numericalId}-${server.uniqueId.substring(0, 8)}").withHostConfig(
                    HostConfig.newHostConfig().withPortBindings(
                        PortBinding(
                            Ports.Binding.bindPort(server.port.toInt()),
                            ExposedPort.tcp(config.exposedPort)
                        )
                    ).withAutoRemove(true)
                )
                .withEnv(env)
                .exec()
            container.warnings.forEach { warning -> logger.warn(warning) }
            containerId = container.id
            server.properties["docker-container-id"] = containerId
            controllerStub.updateServer(updateServerRequest {
                this.server = server.toDefinition()
                this.deleted = false
            })
        } catch (e: Exception) {
            logger.error("Failed to create container for $image:$tag", e)
            return false
        }
        try {
            client.startContainerCmd(containerId).exec()
            val callback = WaitContainerResultCallback()
            client.waitContainerCmd(containerId).exec(callback)
            callback.awaitCompletion()
            logger.info("Server ${server.group}-${server.numericalId} started on Container $containerId")
        } catch (e: Exception) {
            logger.error("Failed to start container $containerId", e)
            return false
        }
        return true
    }

    override suspend fun stopServer(server: Server): Boolean {
        val client = getClientSafe() ?: return false
        val containerId = getContainerId(server) ?: return false
        try {
            client.stopContainerCmd(containerId).exec()
            serverToContainer.keys.find { server.uniqueId == it.uniqueId }?.let { serverToContainer.remove(it) }
            return true
        } catch (e: Exception) {
            logger.error("Failed to stop container $containerId", e)
            return false
        }
    }

    override suspend fun updateServer(server: Server): Server? {
        val client = getClientSafe() ?: return null
        val containerId = getContainerId(server) ?: return null
        try {
            val result = client.inspectContainerCmd(containerId).exec()
            if (result.state.running != true) throw Exception("Controller not running")
            val address = InetSocketAddress(server.ip, server.port.toInt())
            val ping = ServerPinger.ping(address)
            PortProcessHandle.removePreBind(server.port.toInt())
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
            metricsTracker.trackPlayers(copiedServer)
            client.statsCmd(containerId).exec(DockerCallback { stats ->
                val ram = stats.memoryStats.usage ?: 0L
                val cpu = stats.cpuStats.systemCpuUsage ?: 0L
                metricsTracker.trackRamAndCpu(server, ram, cpu)
            })
            return copiedServer
        } catch (e: Exception) {
            logger.error("Failed to update container $containerId", e)
            val portBound = PortProcessHandle.isPortBound(server.port.toInt())
            if (!portBound) {
                stopServer(server)
                return null
            }
            return null
        }
    }

    private fun getContainerId(server: Server): String? {
        if (!server.properties.containsKey("docker-container-id")) {
            logger.error("Server ${server.group}-${server.uniqueId} (${server.uniqueId}) is not running in a docker container")
            return null
        }
        return server.properties["docker-container-id"]!!
    }

    override fun getServer(uniqueId: String): Server? {
        return serverToContainer.keys.find { it.uniqueId == uniqueId }
    }

    override fun reattachServer(server: Server): Boolean {
        val client = getClientSafe() ?: return false
        val containerId = getContainerId(server) ?: return false
        if (containsServer(server)) {
            logger.info("Server ${server.group}-${server.numericalId} was already reattached.")
            return true
        }
        try {
            val state = client.inspectContainerCmd(containerId).exec().state
            if (state.running != true || PortProcessHandle.isPortBound(server.port.toInt())) {
                logger.error("Could not reattach ${server.group}-${server.numericalId}. Is the container down or the port not bound?")
                PortProcessHandle.removePreBind(server.port.toInt())
                return false
            }
        } catch (e: Exception) {
            logger.error("Could not reattach ${server.group}-${server.numericalId}. Container not found.")
            PortProcessHandle.removePreBind(server.port.toInt())
            return false
        }
        logger.info("Server ${server.group}-${server.numericalId} successfully reattached!")
        return true
    }

    override fun executeCommand(server: Server, command: String): Boolean {
        val client = getClientSafe() ?: return false
        val containerId = getContainerId(server) ?: return false
        val statement = client.execCreateCmd(containerId).withCmd(*command.split(" ").toTypedArray()).exec()
        client.execStartCmd(statement.id).exec(DockerCallback {})
        return true
    }

    override fun streamLogs(server: Server): Flow<ServerHostStreamServerLogsResponse> {
        val client = getClientSafe() ?: return emptyFlow()
        val containerId = getContainerId(server) ?: return emptyFlow()
        return client.logContainerCmd(containerId).withFollowStream(true).withTailAll().exec(FlowDockerCallback {
            serverHostStreamServerLogsResponse {
                this.content = String(it.payload)
                this.timestamp = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            }
        }).asFlow()
    }

    override fun appliesFor(env: EnvironmentConfig): Boolean {
        return env.isDocker && getClientSafe() != null
    }

    override fun getServers(): List<Server> {
        return serverToContainer.keys.toList()
    }
}