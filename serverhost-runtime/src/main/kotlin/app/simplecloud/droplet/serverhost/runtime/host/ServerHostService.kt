package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.shared.hack.PortProcessHandle
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import build.buf.gen.simplecloud.controller.v1.*
import io.grpc.Status
import io.grpc.stub.StreamObserver

class ServerHostService(
    private val serverHost: ServerHost,
    private val runner: ServerRunner,
) : ServerHostServiceGrpc.ServerHostServiceImplBase() {
    override fun startServer(request: StartServerRequest, responseObserver: StreamObserver<ServerDefinition>) {
        val group = Group.fromDefinition(request.group)
        val port = PortProcessHandle.findNextFreePort(group.startPort.toInt(), request.server)
        val server = Server.fromDefinition(request.server.copy {
            this.state = ServerState.STARTING
            this.port = port.toLong()
            this.hostId = serverHost.id
            this.ip = serverHost.host
        })

        try {
            val startSuccess = runner.startServer(server)
            if (!startSuccess) {
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

    override fun stopServer(request: ServerDefinition, responseObserver: StreamObserver<ServerDefinition>) {
        runner.stopServer(Server.fromDefinition(request)).thenApply { success ->
            if(!success) {
                responseObserver.onError(
                    Status.INTERNAL
                        .withDescription("Could not stop server")
                        .asRuntimeException()
                )
                return@thenApply
            }
            responseObserver.onNext(request)
            responseObserver.onCompleted()
        }

    }

    override fun reattachServer(request: ServerDefinition, responseObserver: StreamObserver<ServerDefinition>) {
        val server = Server.fromDefinition(request)
        val success = runner.reattachServer(server)
        if(!success) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Could not reattach server")
                    .asRuntimeException()
            )
            return
        }
        responseObserver.onNext(server.toDefinition())
        responseObserver.onCompleted()
    }

}