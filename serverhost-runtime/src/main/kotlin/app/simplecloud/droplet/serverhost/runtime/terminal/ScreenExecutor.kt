package app.simplecloud.droplet.serverhost.runtime.terminal

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.runner.ServerEnvironment

class ScreenExecutor(server: Server, env: ServerEnvironment) : MultiplexerExecutor(server, env) {
    fun setLogsFlush(interval: Int): Boolean {
        if (config == null || !config.isScreen || config.update?.command == null) return false
        return execute(*replaceAndConvert(config.update.command), "logfile", "flush", interval.toString())
    }
}