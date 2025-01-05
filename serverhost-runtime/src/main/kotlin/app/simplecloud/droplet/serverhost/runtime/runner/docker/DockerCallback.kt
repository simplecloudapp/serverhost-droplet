package app.simplecloud.droplet.serverhost.runtime.runner.docker

import com.github.dockerjava.api.async.ResultCallbackTemplate
import java.util.function.Consumer

open class DockerCallback<T>(private val on: Consumer<T>) :
    ResultCallbackTemplate<DockerCallback<T>, T>() {
    override fun onNext(obj: T?) {
        if (obj != null)
            on.accept(obj)
    }
}