package app.simplecloud.droplet.serverhost.shared.grpc

import app.simplecloud.controller.shared.auth.AuthSecretInterceptor
import app.simplecloud.controller.shared.host.ServerHost
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder

object ServerHostGrpc {
    fun createControllerChannel(host: String, port: Int): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext()
            .build()
    }

    fun createGrpcServerBuilder(serverHost: ServerHost, secret: String): ServerBuilder<*> {
        return ServerBuilder.forPort(serverHost.port)
            .intercept(AuthSecretInterceptor(secret))
    }
}