package app.simplecloud.droplet.serverhost.runtime

import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.droplet.serverhost.runtime.host.ServerHostService
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.runtime.template.TemplateCopier
import app.simplecloud.droplet.serverhost.shared.controller.Attacher
import app.simplecloud.droplet.serverhost.shared.grpc.ServerHostGrpc
import app.simplecloud.droplet.serverhost.shared.resources.ResourceCopier
import app.simplecloud.serverhost.configurator.ConfiguratorExecutor
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpcKt
import io.grpc.Server
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager

class ServerHostRuntime(
    private val serverHostStartCommand: ServerHostStartCommand
) {

    private val logger = LogManager.getLogger(ServerHostRuntime::class.java)
    private val authCallCredentials = AuthCallCredentials(serverHostStartCommand.authSecret)

    private val serverHost =
        ServerHost(serverHostStartCommand.hostId, serverHostStartCommand.hostIp, serverHostStartCommand.hostPort)
    private val configurator = ConfiguratorExecutor()
    private val templateCopier = TemplateCopier(serverHostStartCommand)
    private val controllerChannel =
        ServerHostGrpc.createControllerChannel(serverHostStartCommand.grpcHost, serverHostStartCommand.grpcPort)
    private val controllerStub = ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub(controllerChannel)
        .withCallCredentials(authCallCredentials)
    private val runner = ServerRunner(
        configurator,
        templateCopier,
        serverHost,
        serverHostStartCommand,
        controllerStub
    )
    private val server = createGrpcServer()
    private val resourceCopier = ResourceCopier()

    suspend fun start() {
        logger.info("Starting ServerHost ${serverHost.id} on ${serverHost.host}:${serverHost.port}...")
        startGrpcServer()
        attach()
        resourceCopier.copyAll("copy")
        runner.startServerStateChecker()
        templateCopier.loadTemplates()

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

        CoroutineScope(Dispatchers.Default).launch {
            try {
                server.start()
                server.awaitTermination()
            } catch (e: Exception) {
                logger.error("Error in gRPC server", e)
                throw e
            }
        }
    }


    private fun attach() {
        logger.info("Attaching to controller...")
        val attacher =
            Attacher(serverHost, controllerChannel, controllerStub)
        attacher.enforceAttach()
    }

    private fun createGrpcServer(): Server {
        return ServerHostGrpc.createGrpcServerBuilder(serverHost, serverHostStartCommand.authSecret)
            .addService(ServerHostService(serverHost, runner)).build()
    }

}