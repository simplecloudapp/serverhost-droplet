package app.simplecloud.droplet.serverhost.runtime.deployer

import app.simplecloud.controller.shared.server.Server
import org.spongepowered.configurate.objectmapping.ConfigSerializable

interface ServiceConfigDeployer<T: DeploymentConfig> {
    fun deploy(deploymentConfig: T, mapping: DeploymentMapping<T>): Boolean

    fun deploy(server: Server): Boolean
}

@ConfigSerializable
abstract class DeploymentMapping<T>

abstract class DeploymentConfig