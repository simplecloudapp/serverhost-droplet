package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.server.Server

open class ServerHostException(message: String): Exception(message)
class ServerHostStartException(server: Server, message: String): ServerHostException("Server ${server.uniqueId} of group ${server.group} failed to start: $message")