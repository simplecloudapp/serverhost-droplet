package app.simplecloud.droplet.serverhost.runtime.environment.docker

import com.github.dockerjava.api.async.ResultCallbackTemplate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import org.apache.logging.log4j.LogManager

class FlowDockerCallback<T, E>(private val conversion: (T) -> E) : ResultCallbackTemplate<DockerCallback<T>, T>() {

    private val channel = Channel<E>(Channel.UNLIMITED)
    private val logger = LogManager.getLogger(FlowDockerCallback::class.java)

    fun asFlow(): Flow<E> = channel.consumeAsFlow()

    override fun onNext(obj: T?) {
        if (obj != null) {
            // Send the object to the channel
            channel.trySend(conversion(obj)).onFailure {
                // Handle any failure in sending, e.g., if the channel is closed
                logger.error("Failed to send item to channel: ${it?.message}")
            }
        }
    }

    override fun onComplete() {
        super.onComplete()
        channel.close()
    }

    override fun onError(throwable: Throwable?) {
        super.onError(throwable)
        channel.close(throwable)
    }
}