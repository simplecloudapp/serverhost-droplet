package app.simplecloud.droplet.serverhost.runtime

import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.auth.AuthSecretInterceptor
import app.simplecloud.droplet.serverhost.runtime.configurator.ServerConfiguratorExecutor
import app.simplecloud.droplet.serverhost.runtime.controller.Attacher
import app.simplecloud.droplet.serverhost.runtime.host.ServerHostConfig
import app.simplecloud.droplet.serverhost.runtime.host.ServerHostService
import app.simplecloud.droplet.serverhost.runtime.host.ServerVersionLoader
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.resources.ResourceCopier
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.runtime.template.TemplateCopier
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

class ServerHostRuntime(
    private val serverHostStartCommand: ServerHostStartCommand
) {

    private val logger = LogManager.getLogger(ServerHostRuntime::class.java)
    private val authCallCredentials = AuthCallCredentials(serverHostStartCommand.authSecret)

    private val serverHost = ServerHostConfig.load("config.yml")!!
    private val serverLoader = ServerVersionLoader()
    private val configurator = ServerConfiguratorExecutor()
    private val templateCopier = TemplateCopier(serverHostStartCommand)
    private val runner = ServerRunner(
        serverLoader, configurator, templateCopier,
        serverHost, serverHostStartCommand, authCallCredentials
    )
    private val server = createGrpcServerFromEnv()
    private val resourceCopier = ResourceCopier()

    fun start() {
        logger.info("Starting ServerHost ${serverHost.id} on ${serverHost.host}:${serverHost.port}...")
        startGrpcServer()
        attach()
        resourceCopier.copyAll("copy")
        runner.startServerStateChecker()
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
        val attacher = Attacher(authCallCredentials, serverHost)
        attacher.enforceAttach()
    }

    private fun createGrpcServerFromEnv(): Server {
        return ServerBuilder.forPort(serverHost.port)
            .addService(ServerHostService(serverHost, runner))
            .intercept(AuthSecretInterceptor(serverHostStartCommand.authSecret))
            .build()
    }

    companion object {
        fun createControllerChannel(): ManagedChannel {
            val port = System.getenv("GRPC_PORT")?.toInt() ?: 5816
            val host = System.getenv("GRPC_HOST") ?: "localhost"
            return ManagedChannelBuilder.forAddress(host, port).usePlaintext()
                .build()
        }
    }

}