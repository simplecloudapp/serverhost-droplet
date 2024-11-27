package app.simplecloud.droplet.serverhost.shared.template

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTypes
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

object YamlTemplateLoader {


    fun load(
        file: Path,
        actions: Map<String, List<Pair<YamlActionTypes, Any>>>
    ): Pair<List<YamlTemplate>, List<Exception>> {
        if (!file.exists()) throw FileNotFoundException("Template file or folder $file does not exist")
        if (file.isDirectory()) {
            val result = mutableListOf<YamlTemplate>()
            val errors = mutableListOf<Exception>()
            file.listDirectoryEntries("*.yml").map { entry ->
                load(entry, actions)
            }.forEach { template -> result.addAll(template.first); errors.addAll(template.second) }
            return Pair(result, errors)
        }
        val node = YamlConfigurationLoader.builder().path(file).defaultOptions { options ->
            options.serializers { serializers ->
                serializers.registerAll(
                    TypeSerializerCollection.builder()
                        .register(YamlTemplateActionsMap::class.java, YamlTemplateActionSerializer())
                        .registerAnnotatedObjects(
                            objectMapperFactory()
                        ).build()
                )
            }
        }.build().load()

        val name = file.nameWithoutExtension
        val actionMap = node.node("when").get(YamlTemplateActionsMap::class.java)
            ?: InferredYamlTemplateActionsMap(mapOf())

        val errors = actionMap.values.flatMap {
            it.filter { groupId -> !actions.containsKey(groupId) }
                .map { groupId -> NullPointerException("Failed to parse $name: $groupId does not exist") }
        }
        if (errors.isNotEmpty()) {
            return Pair(listOf(), errors)
        }
        val data = node.get(YamlTemplateData::class.java) ?: YamlTemplateData()
        return Pair(listOf(YamlTemplate(name, actionMap, data)), errors)
    }

}