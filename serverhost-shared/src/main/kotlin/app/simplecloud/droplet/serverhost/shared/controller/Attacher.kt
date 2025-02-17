package app.simplecloud.droplet.serverhost.shared.controller

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.droplet.api.droplet.Droplet
import build.buf.gen.simplecloud.controller.v1.ControllerDropletServiceGrpcKt
import build.buf.gen.simplecloud.controller.v1.RegisterDropletRequest
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager

class Attacher(
    private val serverHost: ServerHost,
    private val channel: ManagedChannel,
    private val stub: ControllerDropletServiceGrpcKt.ControllerDropletServiceCoroutineStub,
) {
    private val logger = LogManager.getLogger(Attacher::class.java)

    private var attached: Boolean = false

    private suspend fun attach(): Boolean {
        try {
            stub.registerDroplet(
                RegisterDropletRequest.newBuilder().setDefinition(
                    Droplet(
                        type = "serverhost",
                        host = serverHost.host,
                        id = serverHost.id,
                        port = serverHost.port,
                        envoyPort = 8081
                    ).toDefinition()
                ).build()
            )
            logger.info("Successfully attached to Controller.")
            return true
        } catch (e: Exception) {
            logger.error(e)
            return false
        }
    }

    fun enforceAttach(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
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

    suspend fun enforceAttachBlocking() {
        attached = attach()
        while (!attached) {
            logger.warn("Could not attach to controller, retrying...")
            attached = attach()
            if (attached) {
                return
            }
            delay(5000L)
        }
    }

}