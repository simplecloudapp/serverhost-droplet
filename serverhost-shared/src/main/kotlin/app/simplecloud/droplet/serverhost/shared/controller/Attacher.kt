package app.simplecloud.droplet.serverhost.shared.controller

import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.host.ServerHost
import build.buf.gen.simplecloud.controller.v1.AttachServerHostRequest
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpcKt
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager

class Attacher(
    private val serverHost: ServerHost,
    private val channel: ManagedChannel,
    private val stub: ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub
) {
    private val logger = LogManager.getLogger(Attacher::class.java)

    private suspend fun attach(): Boolean {
        try {
            stub.attachServerHost(AttachServerHostRequest.newBuilder().setServerHost(serverHost.toDefinition()).build())
            logger.info("Successfully attached to Controller.")
            return true
        } catch (e: Exception) {
            logger.error(e)
            return false
        }
    }

    fun enforceAttach(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            var attached = attach()
            while (isActive) {
                if (attached) {
                    if (!channel.getState(true).equals(ConnectivityState.READY)) {
                        attached = false
                    }
                } else {
                    logger.warn("Could not attach to controller, retrying...")
                    attached = attach()
                }
                delay(5000L)
            }
        }
    }

}