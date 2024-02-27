package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.proto.*
import io.grpc.stub.StreamObserver

class ServerHostService() : ServerHostServiceGrpc.ServerHostServiceImplBase() {

    override fun startServer(request: GroupDefinition, responseObserver: StreamObserver<ServerDefinition>) {
        TODO("Not implemented yet")
    }

    override fun stopServer(request: ServerIdRequest, responseObserver: StreamObserver<StatusResponse>) {
        TODO("Not implemented yet")
    }

}