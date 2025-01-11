package app.simplecloud.droplet.serverhost.shared.actions.impl.create.image

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import com.github.dockerjava.api.DockerClient
import com.google.cloud.tools.jib.api.*
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.Port
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

object CreateImageAction : YamlAction<CreateImageActionData> {
    override fun exec(ctx: YamlActionContext, data: CreateImageActionData) {
        val client = ctx.retrieve<DockerClient>("docker-client")
            ?: throw Exception("Can't find docker client for CreateImageAction")
        val serverJar =
            ctx.retrieve<Path>("server-jar") ?: throw Exception("Can't find server jar for CreateImageAction")
        val configuratorJar = ctx.retrieve<Path>("configurator-jar")
            ?: throw Exception("Can't find configurator.jar for CreateImageAction")
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw Exception("Can't find placeholders for CreateImageAction")
        val libs = ctx.retrieve<Path>("libs")
        val context = Paths.get(placeholders.parse(data.context))
        val lastChecksum = ctx.retrieve<String>("last-checksum")
        val checksum = calculateDirectoryChecksum(context)
        if (checksum == lastChecksum) {
            return
        }
        ctx.store("last-checksum", checksum)
        val configuratorYml = Paths.get("options", "configurators", "${placeholders.parse(data.configurator)}.yml")
        val filesBuilder = FileEntriesLayer.builder()
        // Add server.jar
        if (Files.exists(serverJar) && Files.isReadable(serverJar)) {
            filesBuilder.addEntry(serverJar.toAbsolutePath().normalize(), AbsoluteUnixPath.get("/minecraft/server.jar"))
        } else {
            throw Exception("Server jar file does not exist or is not readable: $serverJar")
        }

        // Add configurator.jar
        if (Files.exists(configuratorJar) && Files.isReadable(configuratorJar)) {
            filesBuilder.addEntry(
                configuratorJar.toAbsolutePath().normalize(),
                AbsoluteUnixPath.get("/configurator.jar")
            )
        } else {
            throw Exception("Configurator jar file does not exist or is not readable: $configuratorJar")
        }

        // Add configurator.yml
        if (Files.exists(configuratorYml) && Files.isReadable(configuratorYml)) {
            filesBuilder.addEntry(
                configuratorYml.toAbsolutePath().normalize(),
                AbsoluteUnixPath.get("/configurator.yml")
            )
        } else {
            throw Exception("Configurator.yml file does not exist or is not readable: $configuratorYml")
        }

        if (libs != null && Files.isDirectory(libs) && Files.list(libs).findAny().isPresent) {
            filesBuilder.addEntryRecursive(
                libs.toAbsolutePath().normalize(),
                AbsoluteUnixPath.get("/minecraft/libraries")
            )
        }

        if (Files.exists(context) && Files.isReadable(context) && Files.list(context).findAny().isPresent) {
            filesBuilder.addEntryRecursive(context.toAbsolutePath().normalize(), AbsoluteUnixPath.get("/minecraft"))
        } else {
            throw Exception("Context directory does not exist or is not readable: $context")
        }
        val files = filesBuilder.build()
        val containerizer = createContainerizer(client, placeholders, data)
            ?: throw Exception("Failed to create containerizer for CreateImageAction")
        try {
            Jib.from(ImageReference.parse(data.base))
                .setFileEntriesLayers(files)
                .setEntrypoint("java", "-jar", "configurator.jar", "--working-dir", "/minecraft")
                .setExposedPorts(Port.tcp(25565))
                .containerize(
                    containerizer.setBaseImageLayersCache(
                        Paths.get(
                            "cache",
                            "docker",
                            "base",
                            placeholders.parse(data.base)
                        )
                    ).setApplicationLayersCache(
                        Paths.get(
                            "cache",
                            "docker",
                            "application",
                            placeholders.parse(data.imageName)
                        )
                    ).setAllowInsecureRegistries(true)
                        .setToolName("simplecloud")
                )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun calculateDirectoryChecksum(directory: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")

        // Walk through all files and subdirectories in the directory
        Files.walk(directory)
            .filter { Files.isRegularFile(it) } // Process only regular files
            .sorted() // Ensure deterministic order
            .forEach { file ->
                Files.newInputStream(file).use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead = inputStream.read(buffer)
                    while (bytesRead != -1) {
                        digest.update(buffer, 0, bytesRead)
                        bytesRead = inputStream.read(buffer)
                    }
                }
                // Include file path in the digest to handle file renames
                digest.update(file.toString().toByteArray())
            }

        // Convert the digest to a hex string
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun createContainerizer(
        client: DockerClient,
        placeholders: YamlActionPlaceholderContext,
        data: CreateImageActionData
    ): Containerizer? {
        when (data.buildType) {
            ImageBuildType.TAR -> {
                if (data.tarDest == null) return null
                val dest = Paths.get(placeholders.parse(data.tarDest))
                return Containerizer.to(TarImage.at(dest))
            }

            ImageBuildType.DAEMON -> {
                val containerizer =
                    Containerizer.to(DockerDaemonImage.named(placeholders.parse(data.imageName)))
                data.tags.forEach { containerizer.withAdditionalTag(placeholders.parse(it)) }
                return containerizer
            }

            ImageBuildType.REGISTRY -> {
                val containerizer = Containerizer.to(
                    RegistryImage.named(placeholders.parse(data.imageName))
                        .addCredential(client.authConfig().username, client.authConfig().password)
                )
                data.tags.forEach { containerizer.withAdditionalTag(placeholders.parse(it)) }
                return containerizer
            }

            else -> {
                return null
            }
        }
    }

    override fun getDataType(): Class<CreateImageActionData> {
        return CreateImageActionData::class.java
    }
}