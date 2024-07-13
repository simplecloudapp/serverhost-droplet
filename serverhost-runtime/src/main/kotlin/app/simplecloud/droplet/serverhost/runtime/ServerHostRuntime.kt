package app.simplecloud.droplet.serverhost.runtime

import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.droplet.serverhost.runtime.configurator.ServerConfiguratorExecutor
import app.simplecloud.droplet.serverhost.runtime.host.ServerHostService
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.shared.resources.ResourceCopier
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.runtime.template.TemplateCopier
import app.simplecloud.droplet.serverhost.shared.controller.Attacher
import app.simplecloud.droplet.serverhost.shared.grpc.ServerHostGrpc
import io.grpc.Server
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

class ServerHostRuntime(
    private val serverHostStartCommand: ServerHostStartCommand
) {

    private val logger = LogManager.getLogger(ServerHostRuntime::class.java)
    private val authCallCredentials = AuthCallCredentials(serverHostStartCommand.authSecret)

    private val serverHost =
        ServerHost(serverHostStartCommand.hostId, serverHostStartCommand.hostIp, serverHostStartCommand.hostPort)
    private val configurator = ServerConfiguratorExecutor()
    private val templateCopier = TemplateCopier(serverHostStartCommand)
    private val runner = ServerRunner(
        configurator,
        templateCopier,
        serverHost,
        serverHostStartCommand,
        authCallCredentials
    )
    private val server = createGrpcServer()
    private val resourceCopier = ResourceCopier()

    fun start() {
        logger.info("Starting ServerHost ${serverHost.id} on ${serverHost.host}:${serverHost.port}...")
        startGrpcServer()
        attach()
        resourceCopier.copyAll("copy")
        runner.startServerStateChecker()
        templateCopier.loadTemplates()
    }

    private fun startGrpcServer() {
        logger.info("Starting gRPC server...")
        thread {
            server.start()
            server.awaitTermination()
        }
    }

    private fun attach() {
        logger.info("Attaching to controller...")
        val attacher =
            Attacher(authCallCredentials, serverHost, serverHostStartCommand.grpcHost, serverHostStartCommand.grpcPort)
        attacher.enforceAttach()
    }

    private fun createGrpcServer(): Server {
        return ServerHostGrpc.createGrpcServerBuilder(serverHost, serverHostStartCommand.authSecret)
            .addService(ServerHostService(serverHost, runner)).build()
    }

}