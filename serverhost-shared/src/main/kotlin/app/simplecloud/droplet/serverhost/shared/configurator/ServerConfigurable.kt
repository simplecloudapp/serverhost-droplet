package app.simplecloud.droplet.serverhost.shared.configurator

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.serverhost.configurator.Configurable

class ServerConfigurable(private val server: Server) : Configurable {
    override fun getPlaceholderMappings(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["ip"] = server.ip
        map["port"] = server.port
        map["group"] = server.group
        map["numerical-id"] = server.numericalId
        map["max-players"] = server.maxPlayers
        map.putAll(server.properties)
        return map
    }
}