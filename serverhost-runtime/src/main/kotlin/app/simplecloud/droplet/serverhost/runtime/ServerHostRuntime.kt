package app.simplecloud.droplet.serverhost.runtime

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.droplet.serverhost.runtime.host.ServerHostService
import app.simplecloud.droplet.serverhost.runtime.host.ServerRunner
import app.simplecloud.droplet.serverhost.runtime.host.ServerVersionLoader
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.logging.log4j.LogManager
import java.net.InetAddress
import java.util.*
import kotlin.concurrent.thread

class ServerHostRuntime {

    private val logger = LogManager.getLogger(ServerHostRuntime::class.java)
    private val serverHost = ServerHost(System.getenv("ID") ?: UUID.randomUUID().toString(), InetAddress.getLocalHost().hostAddress, System.getenv("GRPC_PORT")?.toInt() ?: 5820)
    private val serverLoader = ServerVersionLoader()
    private val runner = ServerRunner(serverLoader)
    private val server = createGrpcServerFromEnv()

    fun start() {
        logger.info("Starting ServerHost ${serverHost.id} on ${serverHost.host}:${serverHost.port}...")
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
        return ServerBuilder.forPort(serverHost.port)
                .addService(ServerHostService(serverHost, runner))
                .build()
    }

}