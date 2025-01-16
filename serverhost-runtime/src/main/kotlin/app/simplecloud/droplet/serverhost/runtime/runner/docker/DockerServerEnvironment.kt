package app.simplecloud.droplet.serverhost.runtime.runner.docker

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.api.time.ProtobufTimestamp
import app.simplecloud.droplet.serverhost.runtime.config.environment.*
import app.simplecloud.droplet.serverhost.runtime.config.environment.generators.DefaultDockerConfigGenerator
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.EnvironmentBuilder
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntimeDirectory
import app.simplecloud.droplet.serverhost.runtime.runner.MetricsTracker
import app.simplecloud.droplet.serverhost.runtime.runner.ServerEnvironment
import app.simplecloud.droplet.serverhost.runtime.template.TemplateProvider
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes
import app.simplecloud.droplet.serverhost.shared.docker.DockerClientInstance
import app.simplecloud.droplet.serverhost.shared.hack.PortProcessHandle
import app.simplecloud.droplet.serverhost.shared.hack.ServerPinger
import build.buf.gen.simplecloud.controller.v1.*
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.*
import io.ktor.server.plugins.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.apache.logging.log4j.LogManager
import org.codehaus.plexus.util.cli.CommandLineUtils
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Paths
import java.time.LocalDateTime
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

class DockerServerEnvironment(
    private val serverHost: ServerHost,
    private val args: ServerHostStartCommand,
    private val controllerStub: ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub,
    private val groupStub: ControllerGroupServiceGrpcKt.ControllerGroupServiceCoroutineStub,
    private val metricsTracker: MetricsTracker,
    private val environmentsRepository: EnvironmentConfigRepository,
    private val templateProvider: TemplateProvider,
    override val runtimeRepository: GroupRuntimeDirectory,
) : ServerEnvironment(runtimeRepository, environmentsRepository) {

    private lateinit var dockerClient: DockerClient
    private val logger = LogManager.getLogger(DockerServerEnvironment::class.java)

    private val serverToContainer = mutableMapOf<Server, String>()

    private fun containsServer(server: Server): Boolean {
        return serverToContainer.any { it.key.uniqueId == server.uniqueId }
    }

    override fun getServerCache(): MutableMap<Server, *> {
        return serverToContainer
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

    @OptIn(ExperimentalPathApi::class)
    override fun build(server: Server, context: BuildPolicy) {
        val env = getEnvironment(server) ?: DefaultDockerConfigGenerator.generate(args)
        if (context == BuildPolicy.ONCE && !env.buildPolicy.firstBuild) return
        if (context == BuildPolicy.TRIGGER && !env.buildPolicy.trigger) return
        if (!server.properties.containsKey("docker-image")) {
            logger.error("Can not build a new docker image if none is specified")
            return
        }
        val image = server.properties["docker-image"]!!
        val tag = server.properties["docker-tag"] ?: "latest"
        logger.info("Building image $image:$tag for group ${server.group}...")
        val buildPath = Paths.get("cache", "docker", "build", server.group)
        val result = executeTemplate(
            buildPath,
            Server.fromDefinition(
                server.toDefinition()
                    .copy { this.serverPort = env.start?.docker?.exposedPort?.toLong() ?: this.serverPort }),
            YamlActionTriggerTypes.START,
            templateProvider
        )
        if (result != null) {
            buildPath.deleteRecursively()
            logger.info("Image $image:$tag successfully built!")
        } else {
            logger.error("Failed to build image for group ${server.group}!")
        }
    }

    @OptIn(ExperimentalPathApi::class)
    override suspend fun startServer(server: Server): Boolean {
        val client = getClientSafe() ?: return false
        var config = getEnvironment(server)
        if (config?.enabled == false) {
            logger.info("Environment ${config.name} used in ${server.group} is not enabled")
            return false
        }
        if (!server.properties.containsKey("docker-image")) {
            logger.error("Can not start a server when no docker image is specified")
            return false
        }
        val image = server.properties["docker-image"]!!
        val tag = server.properties["docker-tag"] ?: "latest"
        var imagePresent = DockerUtils.isImagePresent(client, image, tag)
        if (config == null) {
            val runtime = EnvironmentBuilder.buildRuntime(server, runtimeRepository, "docker")
            config = environmentsRepository.get(runtime)
        }
        if (config == null || !config.enabled) {
            logger.info("Environment ${config?.name} used in ${server.group} is not enabled or does not exist.")
            return false
        }
        val buildPath = Paths.get("cache", "docker", "build", server.group)
        val result = if (DockerUtils.shouldBuildImage(config.buildPolicy, imagePresent)) executeTemplate(
            buildPath,
            Server.fromDefinition(
                server.toDefinition()
                    .copy { this.serverPort = config.start?.docker?.exposedPort?.toLong() ?: this.serverPort }),
            YamlActionTriggerTypes.START,
            templateProvider
        ) else null
        val checksum: String
        if (result != null) {
            buildPath.deleteRecursively()
            checksum = result.retrieve<String>("last-checksum") ?: server.properties["last-checksum"] ?: ""
        } else {
            checksum = server.properties["last-checksum"] ?: ""
        }
        if (!imagePresent)
            imagePresent = DockerUtils.isImagePresent(client, image, tag)
        if (DockerUtils.shouldPullImage(config.imagePullPolicy, imagePresent)) {
            val success = DockerUtils.pullImage(client, image, tag)
            if (!success) {
                logger.error("Failed to pull image $image:$tag")
                return false
            }
        }

        val envBuilder = DockerEnvironmentBuilder(config, args, serverHost)
        val env = envBuilder.buildEnv(server)
        val dockerConf = config.start?.docker ?: DockerStartConfig()
        val healthConf = config.start?.docker?.health ?: DockerHealthConfig()
        val healthCommand = healthConf.testCommand.toMutableList()
        envBuilder.addAllWithPlaceholders(healthCommand, mapOf("%EXPOSED_PORT%" to dockerConf.exposedPort.toString()))
        val containerId: String
        val updated: Server
        try {
            val container = client.createContainerCmd("$image:$tag")
                .withName("${server.group}-${server.numericalId}-${server.uniqueId.substring(0, 8)}").withHostConfig(
                    HostConfig.newHostConfig().withPortBindings(
                        PortBinding(
                            Ports.Binding.bindPort(server.port.toInt()),
                            ExposedPort.tcp(dockerConf.exposedPort)
                        )
                    ).withAutoRemove(true)
                ).withHealthcheck(
                    HealthCheck().withInterval(healthConf.interval).withRetries(healthConf.retries)
                        .withTimeout(healthConf.timeout).withTest(healthCommand)
                )
                .withEnv(env)
                .exec()
            container.warnings.forEach { warning -> logger.warn(warning) }
            containerId = container.id
            val group = groupStub.getGroupByName(getGroupByNameRequest { groupName = server.group }).group
            groupStub.updateGroup(updateGroupRequest {
                this.group = group.copy {
                    this.cloudProperties["last-checksum"] = checksum
                }
            })
            updated = Server.fromDefinition(
                server.toDefinition().copy { this.cloudProperties["docker-container-id"] = containerId })
            controllerStub.updateServer(updateServerRequest {
                this.server = updated.toDefinition()
                this.deleted = false
            })

        } catch (e: Exception) {
            logger.error("Failed to create container for $image:$tag", e)
            return false
        }
        try {
            client.startContainerCmd(containerId).exec()
            serverToContainer[updated] = containerId
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
            logger.info("Stopped server ${server.group}-${server.numericalId} (Container: ${containerId})")
            return true
        } catch (e: Exception) {
            logger.error("Failed to stop container $containerId", e)
            return false
        }
    }

    override suspend fun updateServer(server: Server): Server? {
        val client = getClientSafe() ?: return null
        val containerId = getContainerId(server) ?: return null
        val result: InspectContainerResponse
        try {
            result = client.inspectContainerCmd(containerId).exec()
        } catch (e: NotFoundException) {
            logger.error("Server $containerId not running (${server.group}-${server.numericalId})")
            return null
        }
        val healthy = result.state.health != null && result.state.health.failingStreak == 0
        try {
            if (result.state.running != true) return null
            if (!healthy) {
                if (!PortProcessHandle.isPortBound(server.port.toInt())) return null
                return server
            }
            val address = InetSocketAddress(server.ip, server.port.toInt())
            val ping = ServerPinger.ping(address)
            val copiedServer = updateServer(server, ping, controllerStub)
            //metricsTracker.trackPlayers(copiedServer)
            /*client.statsCmd(containerId).exec(DockerCallback { stats ->
                val ram = stats.memoryStats.usage ?: 0L
                val cpu = stats.cpuStats.systemCpuUsage ?: 0L
                metricsTracker.trackRamAndCpu(server, ram, cpu)
            }) */
            return copiedServer
        } catch (e: Exception) {
            if (e !is IOException)
                logger.error("Failed to update container $containerId", e)
            else
                logger.error("Failed to ping container $containerId")
            if (healthy) return server
            return null
        }
    }

    private fun getContainerId(server: Server): String? {
        if (!server.properties.containsKey("docker-container-id")) {
            logger.error("Server ${server.group}-${server.numericalId} (${server.uniqueId}) is not running in a docker container")
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
            if (state.running != true || (state.health.failingStreak > 0 && !PortProcessHandle.isPortBound(server.port.toInt()))) {
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
        serverToContainer[serverToContainer.keys.find { it.uniqueId == server.uniqueId } ?: server] = containerId
        return true
    }

    override fun executeCommand(server: Server, command: String): Boolean {
        val client = getClientSafe() ?: return false
        val containerId = getContainerId(server) ?: return false
        val statement =
            client.execCreateCmd(containerId).withCmd(*CommandLineUtils.translateCommandline(command)).exec()
        client.execStartCmd(statement.id).exec(DockerCallback {})
        return true
    }

    override fun streamLogs(server: Server): Flow<ServerHostStreamServerLogsResponse> {
        val client = getClientSafe() ?: return emptyFlow()
        val containerId = getContainerId(server) ?: return emptyFlow()
        return client.logContainerCmd(containerId).withStdErr(true).withStdOut(true).withFollowStream(true)
            .withTailAll().exec(FlowDockerCallback {
            serverHostStreamServerLogsResponse {
                this.content = String(it.payload)
                this.timestamp = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            }
        }).asFlow()
    }

    override fun appliesFor(env: EnvironmentConfig): Boolean {
        return env.isDocker && getClientSafe() != null
    }

    override fun appliesFor(server: Server): Boolean {
        return server.properties.containsKey("docker-image") && server.properties.containsKey("docker-tag")
    }

    override fun getServers(): List<Server> {
        return serverToContainer.keys.toList()
    }
}