package app.simplecloud.droplet.serverhost.runtime.deployer.server

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.config.YamlConfig
import app.simplecloud.droplet.serverhost.runtime.deployer.DeploymentConfig
import app.simplecloud.droplet.serverhost.runtime.deployer.DeploymentMapping
import app.simplecloud.droplet.serverhost.runtime.deployer.ServiceConfigDeployer
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner
abstract class ServerConfigDeployer<T : ServerDeploymentConfig>(private val path: String) : ServiceConfigDeployer<T> {
    override fun deploy(deploymentConfig: T, mapping: DeploymentMapping<T>): Boolean {
        val dir = ServerRunner.getServerDir(deploymentConfig.getServer())
        val config = YamlConfig(dir.absolutePath)
        config.save(mapping)
        return true
    }

}

abstract class ServerDeploymentMapping<T : ServerDeploymentConfig>(config: T): DeploymentMapping<T>()

abstract class ServerDeploymentConfig(private val server: Server) : DeploymentConfig() {
    fun getServer(): Server {
        return server
    }
}