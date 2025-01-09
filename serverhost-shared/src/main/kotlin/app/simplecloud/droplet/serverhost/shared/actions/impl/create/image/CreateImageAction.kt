package app.simplecloud.droplet.serverhost.shared.actions.impl.create.image

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import com.github.dockerjava.api.DockerClient
import com.google.cloud.tools.jib.api.*
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import java.nio.file.Path
import java.nio.file.Paths

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
        val context = Paths.get(placeholders.parse(data.context))
        val files = FileEntriesLayer.builder().addEntry(serverJar, AbsoluteUnixPath.get("/server.jar"))
            .addEntry(configuratorJar, AbsoluteUnixPath.get("/configurator.jar"))
            .addEntry(context, AbsoluteUnixPath.get("/")).build()
        val containerizer = createContainerizer(client, placeholders, data)
            ?: throw Exception("Failed to create containerizer for CreateImageAction")
        Jib.from(data.base)
            .addFileEntriesLayer(files)
            .setEntrypoint("java", "-jar", "configurator.jar", "-target", "/")
            .addEnvironmentVariable("EULA", "true")
            .containerize(containerizer)
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
                val containerizer = Containerizer.to(DockerDaemonImage.named("${data.imageName}:latest"))
                data.tags.forEach { containerizer.withAdditionalTag(it) }
                return containerizer
            }

            ImageBuildType.REGISTRY -> {
                val containerizer = Containerizer.to(
                    RegistryImage.named(data.imageName)
                        .addCredential(client.authConfig().username, client.authConfig().password)
                )
                data.tags.forEach { containerizer.withAdditionalTag(it) }
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