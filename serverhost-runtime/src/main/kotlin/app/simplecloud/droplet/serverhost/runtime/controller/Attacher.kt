package app.simplecloud.droplet.serverhost.runtime.controller

import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.host.ServerHost
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpc
import app.simplecloud.controller.shared.status.ApiResponse
import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime
import io.grpc.ConnectivityState
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CompletableFuture

class Attacher(private val serverHost: ServerHost) {


    private val channel = ServerHostRuntime.createControllerChannel()
    private val stub = ControllerServerServiceGrpc.newFutureStub(channel)
    private val logger = LogManager.getLogger(Attacher::class.java)

    private fun attach(): CompletableFuture<Boolean> {
        return stub.attachServerHost(serverHost.toDefinition()).toCompletable().thenApply {
            logger.info("Successfully attached to Controller.")
            return@thenApply ApiResponse.fromDefinition(it).status == "success"
        }.exceptionally { return@exceptionally false }
    }


    @OptIn(InternalCoroutinesApi::class)
    fun enforceAttach(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            var attached = attach().get()
            while (NonCancellable.isActive) {
                if (attached) {
                    if (!channel.getState(true).equals(ConnectivityState.READY)) {
                        attached = false
                    }
                } else {
                    logger.warn("Could not attach to controller, retrying...")
                    attached = attach().get()
                }
                delay(5000L)
            }
        }
    }

}