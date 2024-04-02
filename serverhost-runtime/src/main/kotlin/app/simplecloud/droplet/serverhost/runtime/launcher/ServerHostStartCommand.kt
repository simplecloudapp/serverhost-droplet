package app.simplecloud.droplet.serverhost.runtime.launcher

import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

class ServerHostStartCommand : CliktCommand() {

    val templatePath: Path by option(help = "Path to the template files (templates)", envvar = "TEMPLATES_PATH")
        .path()
        .default(Path.of("templates"))
    val runningServersPath by option(help = "Path to the running servers (running)", envvar = "RUNNING_SERVERS_PATH")
        .path()
        .default(Path.of("running"))

    override fun run() {
        val serverHostRuntime = ServerHostRuntime(this)
        serverHostRuntime.start()
    }

}