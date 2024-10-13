package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
import app.simplecloud.droplet.serverhost.shared.hack.PortProcessHandle
import build.buf.gen.simplecloud.controller.v1.*
import io.grpc.Status
import io.grpc.StatusException

class ServerHostService(
    private val serverHost: ServerHost,
    private val runner: ServerRunner,
) : ServerHostServiceGrpcKt.ServerHostServiceCoroutineImplBase() {
    override suspend fun startServer(request: ServerHostStartServerRequest): ServerDefinition {
        val group = Group.fromDefinition(request.group)
        val port = PortProcessHandle.findNextFreePort(group.startPort.toInt(), request.server)
        val server = Server.fromDefinition(request.server.copy {
            this.serverState = ServerState.STARTING
            this.serverPort = port.toLong()
            this.hostId = serverHost.id
            this.serverIp = serverHost.host
        })
        try {
            val started = runner.startServer(server)
            if (!started) {
                throw StatusException(Status.INVALID_ARGUMENT.withDescription("Group not supported by this ServerHost."))
            }
            return server.toDefinition()
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Failed to start server:").withCause(e))
        }
    }

    override suspend fun stopServer(request: ServerDefinition): ServerDefinition {
        val stopped = runner.stopServer(Server.fromDefinition(request))
        if(!stopped) {
            throw StatusException(Status.INTERNAL.withDescription("Could not stop server"))
        }
        return request
    }

    override suspend fun reattachServer(request: ServerDefinition): ServerDefinition {
        val server = Server.fromDefinition(request)
        val success = runner.reattachServer(server)
        if (!success) {
            throw StatusException(Status.INTERNAL.withDescription("Could not reattach server"))
        }
        return server.toDefinition()
    }

}