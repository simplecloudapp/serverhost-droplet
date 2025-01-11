package app.simplecloud.droplet.serverhost.runtime.runner.docker

import com.github.dockerjava.api.DockerClient
import com.nimbusds.jose.util.health.HealthStatus
import io.ktor.server.plugins.*

object DockerUtils {
    fun imageExists(client: DockerClient, name: String, tag: String): Boolean {
        try {
            client.inspectImageCmd("$name:$tag")
            return true
        }catch (e: NotFoundException) {
            return false
        }
    }

    fun isContainerHealthy(client: DockerClient, containerId: String): Boolean {
        val result = client.inspectContainerCmd(containerId).exec()
        return result.state.health.status == "HEALTHY"
    }
}