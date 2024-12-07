package app.simplecloud.droplet.serverhost.shared.grpc

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.droplet.api.auth.AuthSecretInterceptor
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder

object ServerHostGrpc {
    fun createControllerChannel(host: String, port: Int): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext()
            .build()
    }

    fun createGrpcServerBuilder(serverHost: ServerHost, host: String, port: Int): ServerBuilder<*> {
        return ServerBuilder.forPort(serverHost.port).intercept(AuthSecretInterceptor(host, port))
    }
}