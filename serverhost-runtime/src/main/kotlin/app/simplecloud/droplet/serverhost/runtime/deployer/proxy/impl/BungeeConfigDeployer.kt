package app.simplecloud.droplet.serverhost.runtime.deployer.proxy.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.deployer.proxy.ProxyConfigDeployer
import app.simplecloud.droplet.serverhost.runtime.deployer.proxy.ProxyDeploymentConfig
import app.simplecloud.droplet.serverhost.runtime.deployer.proxy.ProxyDeploymentMapping
import org.spongepowered.configurate.objectmapping.meta.Setting

class BungeeConfigDeployer : ProxyConfigDeployer<BungeeDeploymentConfig>("config.yml") {
    override fun deploy(server: Server): Boolean {
        val config = BungeeDeploymentConfig(server)
        return deploy(config, BungeeDeploymentMapping(config))
    }

    override fun deleteChild(server: Server): Boolean {
        val deployment = getDeployment(server) as BungeeDeploymentMapping
        val config = deployment.parent
        config.settings.removePriority(server)
        config.removeChild(server)
        val mapped = createMapping(config)
        return deploy(server, mapped)
    }

    override fun deployChild(server: Server): Boolean {
        val deployment = getDeployment(server) as BungeeDeploymentMapping
        val config = deployment.parent
        config.addChild(server)
        val mapped = createMapping(config)
        return deploy(server, mapped)
    }

    override fun createMapping(config: BungeeDeploymentConfig): ProxyDeploymentMapping<BungeeDeploymentConfig> {
        return BungeeDeploymentMapping(config)
    }

}

class BungeeDeploymentMapping(config: BungeeDeploymentConfig) : ProxyDeploymentMapping<BungeeDeploymentConfig>(config) {
    @Setting("listeners")
    val settings = listOf(config.settings)

    @Setting("servers")
    val children = config.children

    val parent = config
}
class BungeeDeploymentConfig(server: Server) : ProxyDeploymentConfig(server) {
    val settings = BungeeDeployment(server.port.toInt())
    val children = mutableListOf<BungeeServerDeployment>()

    fun removeChild(server: Server) {
        val child = children.first { it.address == "${server.ip}:${server.port}" }
        children.remove(child)
    }

    fun addChild(server: Server) {
        children.add(BungeeServerDeployment(
            "${server.group}-${server.numericalId}",
            "${server.ip}:${server.port}"
        ))
    }
}

data class BungeeDeployment(
    private val port: Int,
    private val host: String = "0.0.0.0:$port",
    private val priorities: MutableList<String> = mutableListOf()
) {
    fun addPriority(server: Server) {
        priorities.add("${server.group}-${server.numericalId}")
    }
    fun removePriority(server: Server) {
        priorities.remove("${server.group}-${server.numericalId}")
    }
}

data class BungeeServerDeployment(
    val motd: String,
    val address: String,
    val restricted: Boolean = false,
)