package app.simplecloud.droplet.serverhost.runtime

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.droplet.api.auth.AuthCallCredentials
import app.simplecloud.droplet.serverhost.runtime.config.environment.EnvironmentConfigRepository
import app.simplecloud.droplet.serverhost.runtime.files.FileSystemSnapshotCache
import app.simplecloud.droplet.serverhost.runtime.host.ServerHostService
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.GroupRuntimeDirectory
import app.simplecloud.droplet.serverhost.runtime.runner.MetricsTracker
import app.simplecloud.droplet.serverhost.runtime.runner.ServerEnvironments
import app.simplecloud.droplet.serverhost.runtime.template.ActionProvider
import app.simplecloud.droplet.serverhost.runtime.template.TemplateProvider
import app.simplecloud.droplet.serverhost.shared.controller.Attacher
import app.simplecloud.droplet.serverhost.shared.grpc.ServerHostGrpc
import app.simplecloud.droplet.serverhost.shared.resources.ResourceCopier
import app.simplecloud.pubsub.PubSubClient
import build.buf.gen.simplecloud.controller.v1.ControllerDropletServiceGrpcKt
import build.buf.gen.simplecloud.controller.v1.ControllerGroupServiceGrpcKt
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpcKt
import io.grpc.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class ServerHostRuntime(
    private val serverHostStartCommand: ServerHostStartCommand
) {

    private val logger = LogManager.getLogger(ServerHostRuntime::class.java)
    private val authCallCredentials = AuthCallCredentials(serverHostStartCommand.authSecret)

    private val serverHost =
        ServerHost(serverHostStartCommand.hostId, serverHostStartCommand.hostIp, serverHostStartCommand.hostPort)
    private val actionProvider =
        ActionProvider(
            Path.of(serverHostStartCommand.templateDefinitionPath.absolutePathString(), "actions")
        )
    private val templateProvider = TemplateProvider(
        serverHostStartCommand,
        actionProvider
    )
    private val templateSnapshotCache = FileSystemSnapshotCache(serverHostStartCommand.templatePath)
    private val environmentsRepository = EnvironmentConfigRepository(serverHostStartCommand)
    private val controllerChannel =
        ServerHostGrpc.createControllerChannel(serverHostStartCommand.grpcHost, serverHostStartCommand.grpcPort)
    private val controllerStub = ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub(controllerChannel)
        .withCallCredentials(authCallCredentials)
    private val groupStub = ControllerGroupServiceGrpcKt.ControllerGroupServiceCoroutineStub(controllerChannel)
        .withCallCredentials(authCallCredentials)
    private val controllerDropletStub =
        ControllerDropletServiceGrpcKt.ControllerDropletServiceCoroutineStub(controllerChannel)
            .withCallCredentials(authCallCredentials)
    private val pubSubClient =
        PubSubClient(serverHostStartCommand.pubSubGrpcHost, serverHostStartCommand.pubSubGrpcPort, authCallCredentials)
    private val environments = ServerEnvironments(
        templateProvider,
        serverHost,
        serverHostStartCommand,
        controllerStub,
        groupStub,
        MetricsTracker(pubSubClient),
        environmentsRepository,
        GroupRuntimeDirectory()
    )
    private val server = createGrpcServer()
    private val resourceCopier = ResourceCopier()

    suspend fun start() {
        logger.info("Starting ServerHost ${serverHost.id} on ${serverHost.host}:${serverHost.port}...")
        startGrpcServer()
        attach()
        resourceCopier.copyAll("copy")
        actionProvider.load()
        templateProvider.load()
        startFileSystemWatcher()
        environments.startServerStateChecker()
        suspendCancellableCoroutine<Unit> { continuation ->
            Runtime.getRuntime().addShutdownHook(Thread {
                server.shutdown()
                continuation.resume(Unit) { cause, _, _ ->
                    logger.info("Server shutdown due to: $cause")
                }
            })
        }
    }

    private fun startGrpcServer() {
        logger.info("Starting gRPC server...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                server.start()
                server.awaitTermination()
            } catch (e: Exception) {
                logger.error("Error in gRPC server", e)
                throw e
            }
        }
    }

    private fun startFileSystemWatcher() {
        logger.info("Starting template file system watcher...")
        templateSnapshotCache.registerWatcher()
    }


    private fun attach() {
        logger.info("Attaching to controller...")
        val attacher =
            Attacher(serverHost, controllerChannel, controllerDropletStub)
        attacher.enforceAttach()
    }

    private fun createGrpcServer(): Server {
        return ServerHostGrpc.createGrpcServerBuilder(
            serverHost,
            serverHostStartCommand.grpcHost,
            serverHostStartCommand.authorizationPort
        )
            .addService(ServerHostService(serverHost, environments, templateSnapshotCache, serverHostStartCommand))
            .build()
    }

}