package app.simplecloud.droplet.serverhost.shared.server

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.proto.ServerState
import app.simplecloud.controller.shared.server.Server
import java.util.*
import kotlin.properties.Delegates

class ServerFactory {

    companion object {
        fun builder(): ServerFactory {
            return ServerFactory()
        }
    }

    private lateinit var group: Group

    fun setGroup(group: Group): ServerFactory {
        this.group = group
        return this
    }

    private lateinit var host: ServerHost

    fun setHost(host: ServerHost): ServerFactory {
        this.host = host
        return this
    }

    private var numericalId by Delegates.notNull<Long>()

    fun setNumericalId(numericalId: Long): ServerFactory {
        this.numericalId = numericalId
        return this
    }
    private var port by Delegates.notNull<Long>()
    fun setPort(port: Long): ServerFactory {
        this.port = port
        return this
    }

    fun build(): Server {
        return Server(
            uniqueId = UUID.randomUUID().toString().replace("-", ""),
            port = port,
            group = group.name,
            minMemory = group.minMemory,
            maxMemory = group.maxMemory,
            host = host.id,
            ip = host.host,
            state = ServerState.STARTING,
            numericalId = numericalId.toInt(),
            playerCount = 0,
            templateId = "",
            properties = mutableMapOf(
                "serverUrl" to group.serverUrl,
                *group.properties.entries.map { it.key to it.value }.toTypedArray()
            )
        )
    }

}