package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.proto.*
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.status.ApiResponse
import app.simplecloud.droplet.serverhost.runtime.hack.PortProcessHandle
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.shared.server.ServerFactory
import io.grpc.stub.StreamObserver

class ServerHostService(
    private val serverHost: ServerHost,
    private val runner: ServerRunner,
) : ServerHostServiceGrpc.ServerHostServiceImplBase() {

    override fun startServer(request: StartServerRequest, responseObserver: StreamObserver<ServerDefinition>) {
        val group = Group.fromDefinition(request.group)
        val port = PortProcessHandle.findNextFreePort(group.startPort.toInt())
        PortProcessHandle.addPreBind(port)
        val server = ServerFactory.builder()
            .setHost(serverHost)
            .setGroup(group)
            .setNumericalId(request.numericalId.toLong())
            .setPort(port.toLong())
            .build()
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
        responseObserver.onNext(ApiResponse(if (runner.stopServer(Server.fromDefinition(request))) "success" else "error").toDefinition())
        responseObserver.onCompleted()
    }

    override fun reattachServer(request: ServerDefinition, responseObserver: StreamObserver<StatusResponse>) {
        responseObserver.onNext(ApiResponse(if (runner.reattachServer(Server.fromDefinition(request))) "success" else "error").toDefinition())
        responseObserver.onCompleted()
    }

}