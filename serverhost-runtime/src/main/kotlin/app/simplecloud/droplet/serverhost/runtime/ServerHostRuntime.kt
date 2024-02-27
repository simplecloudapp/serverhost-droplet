package app.simplecloud.droplet.serverhost.runtime

import app.simplecloud.droplet.serverhost.runtime.host.ServerHostService
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

class ServerHostRuntime {

    private val logger = LogManager.getLogger(ServerHostRuntime::class.java)
    private val server = createGrpcServerFromEnv()

    fun start() {
        logger.info("Starting ServerHost...")
        startGrpcServer()
    }

    private fun startGrpcServer() {
        logger.info("Starting gRPC server...")
        thread {
            server.start()
            server.awaitTermination()
        }
    }

    private fun createGrpcServerFromEnv(): Server {
        val port = System.getenv("GRPC_PORT")?.toInt() ?: 5816
        return ServerBuilder.forPort(port)
                .addService(ServerHostService())
                .build()
    }

}