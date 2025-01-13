package app.simplecloud.droplet.serverhost.runtime.runner.docker

import app.simplecloud.droplet.serverhost.runtime.config.environment.BuildPolicy
import app.simplecloud.droplet.serverhost.runtime.config.environment.ImagePullPolicy
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.PullResponseItem
import io.ktor.server.plugins.*

object DockerUtils {

    fun isContainerHealthy(client: DockerClient, containerId: String): Boolean {
        val result = client.inspectContainerCmd(containerId).exec()
        return result.state.health != null && result.state.health.failingStreak == 0
    }

    fun isImagePresent(client: DockerClient, image: String, tag: String = "latest"): Boolean {
        try {
            val result = client.inspectImageCmd(image).exec()
            return result.id != null && (result.repoTags ?: listOf()).contains("$image:$tag")
        }catch (e: NotFoundException) {
            return false
        }
    }

    fun pullImage(client: DockerClient, image: String, tag: String = "latest"): Boolean {
        var success = true
        val callback = object : PullImageResultCallback() {
            override fun onNext(item: PullResponseItem?) {
                super.onNext(item)
                if (item == null) return
                if (!item.isPullSuccessIndicated) success = false
            }
        }

        client.pullImageCmd("$image:$tag").exec(callback)
        callback.awaitCompletion()
        return success
    }

    fun shouldBuildImage(buildPolicy: BuildPolicy, present: Boolean): Boolean {
        if(buildPolicy == BuildPolicy.ALWAYS) return true
        if(buildPolicy.firstBuild && !present) return true
        return false
    }

    fun shouldPullImage(pullPolicy: ImagePullPolicy, present: Boolean): Boolean {
        return when (pullPolicy) {
            ImagePullPolicy.ALWAYS -> true
            ImagePullPolicy.IF_NOT_PRESENT -> !present
            ImagePullPolicy.NEVER -> false
        }
    }
}