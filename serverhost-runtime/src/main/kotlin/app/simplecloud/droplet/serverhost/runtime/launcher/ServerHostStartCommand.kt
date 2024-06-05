package app.simplecloud.droplet.serverhost.runtime.launcher

import app.simplecloud.controller.shared.secret.AuthFileSecretFactory
import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import java.net.InetAddress
import java.nio.file.Path

class ServerHostStartCommand : CliktCommand() {

    val hostId: String by option(help = "ServerHost ID", envvar = "HOST_ID").default("internal-server-host")
    val hostIp: String by option(help = "ServerHost IP (default: local host address)", envvar = "HOST_IP").default(
        InetAddress.getLocalHost().hostAddress)
    val hostPort: Int by option(help = "ServerHost port (default: 5820)", envvar = "HOST_PORT").int().default(5820)

    val templatePath: Path by option(help = "Path to the template files (templates)", envvar = "TEMPLATES_PATH")
        .path()
        .default(Path.of("templates"))
    val runningServersPath by option(help = "Path to the running servers (running)", envvar = "RUNNING_SERVERS_PATH")
        .path()
        .default(Path.of("running"))

    val grpcHost: String by option(help = "Grpc host (default: localhost)", envvar = "GRPC_HOST").default("localhost")
    val grpcPort: Int by option(help = "Grpc port (default: 5816)", envvar = "GRPC_PORT").int().default(5816)

    private val authSecretPath: Path by option(
        help = "Path to auth secret file (default: .auth.secret)",
        envvar = "AUTH_SECRET_PATH"
    )
        .path()
        .default(Path.of(".secrets", "auth.secret"))

    val authSecret: String by option(help = "Auth secret", envvar = "AUTH_SECRET_KEY")
        .defaultLazy { AuthFileSecretFactory.loadOrCreate(authSecretPath) }

    override fun run() {
        val serverHostRuntime = ServerHostRuntime(this)
        serverHostRuntime.start()
    }

}