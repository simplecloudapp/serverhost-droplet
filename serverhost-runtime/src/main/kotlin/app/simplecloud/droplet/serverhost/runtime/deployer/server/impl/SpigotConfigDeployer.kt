package app.simplecloud.droplet.serverhost.runtime.deployer.server.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.deployer.server.ServerConfigDeployer
import app.simplecloud.droplet.serverhost.runtime.deployer.server.ServerDeploymentConfig
import app.simplecloud.droplet.serverhost.runtime.deployer.server.ServerDeploymentMapping
import org.spongepowered.configurate.objectmapping.meta.Setting

class SpigotConfigDeployer : ServerConfigDeployer<SpigotDeploymentConfig>("server.properties") {
    override fun deploy(server: Server): Boolean {
        val config = SpigotDeploymentConfig(server)
        return deploy(config, SpigotDeploymentMapping(config))
    }
}

class SpigotDeploymentMapping(config: SpigotDeploymentConfig): ServerDeploymentMapping<SpigotDeploymentConfig>(config) {
    @Setting("rcon.port")
    val rconPort = config.port

    @Setting("server-port")
    val serverPort = config.port
}
class SpigotDeploymentConfig(server: Server): ServerDeploymentConfig(server) {
    val port = server.port
}