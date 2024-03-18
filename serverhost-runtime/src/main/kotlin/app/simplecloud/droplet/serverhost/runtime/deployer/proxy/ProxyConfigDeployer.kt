package app.simplecloud.droplet.serverhost.runtime.deployer.proxy

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.config.YamlConfig
import app.simplecloud.droplet.serverhost.runtime.deployer.DeploymentConfig
import app.simplecloud.droplet.serverhost.runtime.deployer.DeploymentMapping
import app.simplecloud.droplet.serverhost.runtime.deployer.ServiceConfigDeployer
import app.simplecloud.droplet.serverhost.runtime.runner.ServerRunner

abstract class ProxyConfigDeployer<T : ProxyDeploymentConfig>(private val path: String) : ServiceConfigDeployer<T> {
    override fun deploy(deploymentConfig: T, mapping: DeploymentMapping<T>): Boolean {
        val dir = ServerRunner.getServerDir(deploymentConfig.getServer())
        val config = YamlConfig(dir.absolutePath)
        config.save(path, mapping)
        return true
    }

    fun deploy(server: Server, mapping: DeploymentMapping<T>): Boolean {
        val dir = ServerRunner.getServerDir(server)
        val config = YamlConfig(dir.absolutePath)
        config.save(path, mapping)
        return true
    }
    abstract fun createMapping(config: T): ProxyDeploymentMapping<T>
    abstract fun deleteChild(server: Server): Boolean
    abstract fun deployChild(server: Server): Boolean

    fun getDeployment(server: Server): ProxyDeploymentMapping<T>? {
        val dir = ServerRunner.getServerDir(server)
        return YamlConfig(dir.absolutePath).load<ProxyDeploymentMapping<T>>(path)
    }
}
abstract class ProxyDeploymentMapping<T : ProxyDeploymentConfig>(config: T): DeploymentMapping<T>()
abstract class ProxyDeploymentConfig(private val server: Server) : DeploymentConfig() {
    fun getServer(): Server {
        return server
    }
}