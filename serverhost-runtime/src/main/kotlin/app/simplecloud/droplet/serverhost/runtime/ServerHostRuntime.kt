package app.simplecloud.droplet.serverhost.runtime

import app.simplecloud.droplet.serverhost.runtime.controller.Attacher
import app.simplecloud.droplet.serverhost.runtime.host.ServerHostConfig
import app.simplecloud.droplet.serverhost.runtime.host.ServerHostService
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.runtime.host.ServerVersionLoader
import app.simplecloud.droplet.serverhost.runtime.template.TemplateCopier
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

class ServerHostRuntime {

    private val logger = LogManager.getLogger(ServerHostRuntime::class.java)
    private val serverHost = ServerHostConfig.load("config.yml")
    private val serverLoader = ServerVersionLoader()
    private val templateCopier = TemplateCopier()
    private val runner = ServerRunner(serverLoader, templateCopier)
    private val server = createGrpcServerFromEnv()

    fun start() {
        if (serverHost == null) {
            logger.error("This ServerHost is unusable, since no config was provided.")
            return
        }
        logger.info("Starting ServerHost ${serverHost.id} on ${serverHost.host}:${serverHost.port}...")
        startGrpcServer()
        attach()
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
        val attacher = Attacher(serverHost!!)
        attacher.enforceAttach()
    }

    private fun createGrpcServerFromEnv(): Server {
        return ServerBuilder.forPort(serverHost!!.port)
            .addService(ServerHostService(serverHost, runner))
            .build()
    }

}