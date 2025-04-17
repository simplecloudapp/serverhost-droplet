package app.simplecloud.droplet.serverhost.runtime.launcher

import app.simplecloud.droplet.api.secret.AuthFileSecretFactory
import app.simplecloud.droplet.serverhost.runtime.ServerHostRuntime
import app.simplecloud.metrics.internal.api.MetricsCollector
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.clikt.sources.ValueSource
import java.io.File
import java.net.InetAddress
import java.nio.file.Path

class ServerHostStartCommand(
    private val metricsCollector: MetricsCollector?
) : SuspendingCliktCommand() {
    init {
        context {
            valueSource = PropertiesValueSource.from(File("serverhost.properties"), false, ValueSource.envvarKey())
        }
    }

    val hostId: String by option(help = "ServerHost ID", envvar = "HOST_ID").default("internal-server-host")
    val hostIp: String by option(help = "ServerHost IP (default: local host address)", envvar = "HOST_IP").default(
        try {
            InetAddress.getLocalHost().hostAddress
        } catch (e: Exception) {
            "127.0.0.1"
        }
    )
    val hostPort: Int by option(help = "ServerHost port (default: 5820)", envvar = "HOST_PORT").int().default(5820)
    
    val templatePath: Path by option(help = "Path to the template files (templates)", envvar = "TEMPLATES_PATH")
        .path()
        .default(Path.of("templates"))
    val templateDefinitionPath: Path by option(
        help = "Path to the template definition files (actions and templates)",
        envvar = "TEMPLATE_DEFINITIONS_PATH"
    )
        .path()
        .default(Path.of("templates"))
    val environmentsPath: Path by option(
        help = "Path to the environment definition files (environments)",
        envvar = "ENVIRONMENTS_PATH"
    )
        .path()
        .default(Path.of("environments"))
    val runningServersPath by option(help = "Path to the running servers (running)", envvar = "RUNNING_SERVERS_PATH")
        .path()
        .default(Path.of("running"))
    val logsPath by option(help = "Path to the logs files (logs)", envvar = "LOGS_PATH").path()
        .default(Path.of("logs", "servers"))
    val grpcHost: String by option(help = "Grpc host (default: localhost)", envvar = "GRPC_HOST").default("localhost")
    val grpcPort: Int by option(help = "Grpc port (default: 5816)", envvar = "GRPC_PORT").int().default(5816)

    val pubSubGrpcHost: String by option(
        help = "PubSub Grpc host (default: localhost)",
        envvar = "CONTROLLER_PUBSUB_HOST"
    ).default("localhost")
    val pubSubGrpcPort: Int by option(
        help = "PubSub Grpc port (default: 5817)",
        envvar = "CONTROLLER_PUBSUB_PORT"
    ).int().default(5817)

    private val authSecretPath: Path by option(
        help = "Path to auth secret file (default: .auth.secret)",
        envvar = "AUTH_SECRET_PATH"
    )
        .path()
        .default(Path.of(".secrets", "auth.secret"))

    val dockerConfigPath: Path by option(
        help = "Path to docker config file (default: docker.properties)",
        envvar = "DOCKER_CONFIG_PATH"
    )
        .path()
        .default(Path.of("docker.properties"))

    val authSecret: String by option(help = "Auth secret", envvar = "AUTH_SECRET_KEY")
        .defaultLazy { AuthFileSecretFactory.loadOrCreate(authSecretPath) }

    val authorizationPort: Int by option(
        help = "Authorization port (default: 5818)",
        envvar = "AUTHORIZATION_PORT"
    ).int().default(5818)

    private val forwardingSecretPath: Path by option(
        help = "Path to forwarding secret file (default: forwarding.secret)",
        envvar = "FORWARDING_SECRET_PATH"
    )
        .path()
        .default(Path.of(".secrets", "forwarding.secret"))

    val forwardingSecret: String by option(help = "Forwarding secret", envvar = "FORWARDING_SECRET_KEY")
        .defaultLazy { AuthFileSecretFactory.loadOrCreate(forwardingSecretPath) }

    val trackMetrics: Boolean by option(help = "Track metrics", envvar = "TRACK_METRICS")
        .boolean()
        .default(true)

    override suspend fun run() {
        if (trackMetrics) {
            metricsCollector?.start()
        }

        val serverHostRuntime = ServerHostRuntime(this)
        serverHostRuntime.start()
    }

}