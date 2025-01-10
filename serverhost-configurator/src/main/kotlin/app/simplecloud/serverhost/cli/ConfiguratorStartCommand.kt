package app.simplecloud.serverhost.cli

import app.simplecloud.serverhost.configurator.ConfiguratorExecutor
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import org.codehaus.plexus.util.cli.CommandLineUtils
import java.nio.file.Path

class ConfiguratorStartCommand : CliktCommand() {

    private val destinationPath: Path by option(
        help = "Path to execute the configurator against (path)", envvar = "DESTINATION_PATH"
    ).path().default(Path.of(""))
    private val configuratorPath: Path by option(
        help = "Path to execute the configurator against (path)", envvar = "CONFIGURATOR_PATH"
    ).path().default(Path.of("configurator.yml"))
    val prefix: String by option(
        help = "Prefix of simplecloud environment variables (string)", envvar = "SIMPLECLOUD_PREFIX"
    ).default("SIMPLECLOUD_")
    private val forwardingSecret: String? by option(
        help = "Forwarding secret used with velocity (string)", envvar = "FORWARDING_SECRET"
    )
    val command: String? by option(
        help = "Command to execute after successful configuration (string)", envvar = "SIMPLECLOUD_COMMAND"
    )

    override fun run() {
        try {
            val executor = ConfiguratorExecutor()
            val result = executor.configurateFile(
                EnvConfigurable(this), configuratorPath, destinationPath.toFile(), forwardingSecret ?: ""
            )
            if (!result) {
                println("Configurator failed.")
                return
            }
            if (command != null) {
                println("Successfully configured, executing command...")
                val process = ProcessBuilder().command(*CommandLineUtils.translateCommandline(command))
                    .directory(destinationPath.toFile()).inheritIO().start()
                println("\"${command}\" now running on ${process.pid()}")
                val returnCode = process.waitFor()
                println("\"${command}\" terminated with exit code $returnCode.")
                return
            }

            println("Successfully configured!")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}