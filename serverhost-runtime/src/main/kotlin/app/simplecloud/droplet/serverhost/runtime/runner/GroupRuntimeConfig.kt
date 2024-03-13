package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.droplet.serverhost.runtime.host.ServerRunner
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

@ConfigSerializable
data class GroupRuntimeConfig(val jvm: JvmArguments? = null, val ignore: Boolean? = false, val parentDir: String?) {
    companion object {
        fun load(group: Group): GroupRuntimeConfig? {
            return load(group.name)
        }

        private fun buildNode(group: String): CommentedConfigurationNode {
            val file = File("options/${group}.yml")
            if(!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            val loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .defaultOptions { options ->
                    options.serializers{ builder ->
                        builder.registerAnnotatedObjects(objectMapperFactory())
                    }
                }.build()
            return loader.load()
        }

        fun load(group: String) : GroupRuntimeConfig? {
            val node = buildNode(group)
            return node.get<GroupRuntimeConfig>()
        }
    }

    fun save(group: Group) {
        save(group.name)
    }

    fun save(group: String) {
        val node = buildNode(group)
    }
}
