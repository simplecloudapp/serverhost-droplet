package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.proto.*
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.status.ApiResponse
import app.simplecloud.droplet.serverhost.runtime.hack.PortProcessHandle
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import io.grpc.stub.StreamObserver
import java.time.ZoneId

class ServerHostService(
    private val serverHost: ServerHost,
    private val runner: ServerRunner,
) : ServerHostServiceGrpc.ServerHostServiceImplBase() {

    override fun startServer(request: StartServerRequest, responseObserver: StreamObserver<ServerDefinition>) {
        val group = Group.fromDefinition(request.group)
        val port = PortProcessHandle.findNextFreePort(group.startPort.toInt())
        val server = Server.fromDefinition(request.server.copy {
            this.state = ServerState.STARTING
            this.port = port.toLong()
            this.hostId = serverHost.id
            this.ip = serverHost.host
        })
        PortProcessHandle.addPreBind(port, server.createdAt, server.properties.getOrDefault("max-startup-seconds", "20").toLong())
        try {
            if (!runner.startServer(server)) {
                responseObserver.onError(ServerHostStartException(server, "Group not supported by this ServerHost."))
                return
            }
        } catch (e: Exception) {
            responseObserver.onError(ServerHostStartException(server, "An internal error occurred."))
            e.printStackTrace()
            return
        }
        responseObserver.onNext(server.toDefinition())
        responseObserver.onCompleted()
    }

    override fun stopServer(request: ServerDefinition, responseObserver: StreamObserver<StatusResponse>) {
        runner.stopServer(Server.fromDefinition(request)).thenApply {
            responseObserver.onNext(ApiResponse(if (it) "success" else "error").toDefinition())
            responseObserver.onCompleted()
        }

    }

    override fun reattachServer(request: ServerDefinition, responseObserver: StreamObserver<StatusResponse>) {
        responseObserver.onNext(ApiResponse(if (runner.reattachServer(Server.fromDefinition(request))) "success" else "error").toDefinition())
        responseObserver.onCompleted()
    }

}