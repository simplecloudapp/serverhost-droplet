package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.droplet.serverhost.runtime.config.YamlConfig
import java.io.File
import java.net.InetAddress
import java.nio.file.Files

class ServerHostConfig {
    companion object {
        fun load(path: String): ServerHost? {
            val envHost = fromEnv()
            if (envHost != null) return envHost
            if (!File(path).exists()) {
                Files.copy(ServerHostConfig::class.java.getResourceAsStream("/$path")!!, File(path).toPath())
            }
            return YamlConfig(path).load()
        }

        private fun fromEnv(): ServerHost? {
            val id = System.getenv("ID") ?: return null
            val port = System.getenv("HOST_PORT")?.toInt() ?: 5820
            val host = System.getenv("HOST_IP") ?: InetAddress.getLocalHost().hostAddress
            return ServerHost(id, host, port)
        }
    }
}