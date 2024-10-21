package app.simplecloud.droplet.serverhost.runtime.configurator

import app.simplecloud.controller.shared.server.Server

object ServerConfiguratorPlaceholderMapper {
    private fun toTemplateList(server: Server, forwardingSecret: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["ip"] = server.ip
        map["port"] = server.port
        map["group"] = server.group
        map["numerical-id"] = server.numericalId
        map["max-players"] = server.maxPlayers
        map["forwarding-secret"] = forwardingSecret
        map.putAll(server.properties)
        return map
    }

    fun map(templatedString: String, server: Server, forwardingSecret: String): String {
        var result = templatedString
        for ((template, value) in toTemplateList(server, forwardingSecret)) {
            result = result.replace("%$template%", value.toString())
        }
        return result
    }
}