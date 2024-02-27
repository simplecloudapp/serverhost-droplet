package app.simplecloud.droplet.serverhost.runtime.launcher

import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime

fun main() {
    val serverHost = ServerHostRuntime()
    serverHost.start()
}