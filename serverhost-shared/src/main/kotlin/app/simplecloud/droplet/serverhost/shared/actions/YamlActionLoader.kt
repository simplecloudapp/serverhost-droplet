package app.simplecloud.droplet.serverhost.shared.actions

import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path
import java.util.function.Consumer
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

class YamlActionLoader {


    fun load(directory: Path) {
        var actionFiles = loadActionFiles(directory)
        //This will now deserialize all the groups that were loaded and update the ist
        actionFiles = loadActionGroups(directory, actionFiles)
    }

    fun loadActionGroups(directory: Path, actionFiles: List<YamlActionFile>): List<YamlActionFile> {
        return loadActionGroupsRef(directory, loadActionGroupsData(directory, actionFiles))
    }

    //Preload all the static data to make referencing easier
    private fun loadActionGroupsData(directory: Path, actionFiles: List<YamlActionFile>): List<YamlActionFile> {
        val returned = mutableListOf<YamlActionFile>()
        walkActionFiles(directory) { entry ->
            val actionFile = actionFiles.firstOrNull { it.fileName == entry.nameWithoutExtension } ?: return@walkActionFiles
            val serializedGroups = mutableListOf<YamlActionGroup>()
            for (actionGroup in actionFile.groups) {
                serializedGroups.add(deserializeGroup(entry, actionFiles, actionFile, actionGroup, YamlActionDataDescriptor.DATA))
            }
            returned.add(YamlActionFile(actionFile.fileName, serializedGroups))
        }
        return returned
    }

    //TODO: THIS WILL NOT WORK WITH DEEPER REFS! PAIR PROGRAMMING SESSION NEEDED!
    private fun loadActionGroupsRef(directory: Path, actionFiles: List<YamlActionFile>): List<YamlActionFile> {
        val returned = mutableListOf<YamlActionFile>()
        walkActionFiles(directory) { entry ->
            val actionFile = actionFiles.firstOrNull { it.fileName == entry.nameWithoutExtension } ?: return@walkActionFiles
            val serializedGroups = mutableListOf<YamlActionGroup>()
            for (actionGroup in actionFile.groups) {
                serializedGroups.add(deserializeGroup(entry, actionFiles, actionFile, actionGroup, YamlActionDataDescriptor.REF))
            }
            returned.add(YamlActionFile(actionFile.fileName, serializedGroups))
        }
        return returned
    }

    fun loadActionFiles(directory: Path): List<YamlActionFile> {
        val actionFiles = mutableListOf<YamlActionFile>()
        walkActionFiles(directory) { entry ->
            val fileName = entry.nameWithoutExtension
            val groups = scanFileForGroups(entry)
            actionFiles.add(YamlActionFile(fileName, groups))
        }
        return actionFiles
    }

    private fun walkActionFiles(directory: Path, consumer: Consumer<Path>) {
        if (!directory.isDirectory()) throw IllegalArgumentException("Not a directory: $directory")
        for (entry in directory.listDirectoryEntries("*.yml")) {
            consumer.accept(entry)
        }
    }

    private fun deserializeGroup(
        file: Path,
        actionFiles: List<YamlActionFile>,
        actionFile: YamlActionFile,
        actionGroup: YamlActionGroup,
        type: YamlActionDataDescriptor
    ): YamlActionGroup {
        val loader = YamlConfigurationLoader.builder().path(file).defaultOptions { opts ->
            opts.serializers { serializers ->
                serializers.registerAll(
                    TypeSerializerCollection.builder()
                        .register(
                        YamlActionGroup::class.java,
                        YamlActionGroupSerializer(actionFiles, actionFile, actionGroup, type)
                    ).registerAnnotatedObjects(objectMapperFactory())
                        .build()
                )
            }
        }.build()
        val node = loader.load().node(actionGroup.name)
        return node.get(YamlActionGroup::class.java) ?: throw SerializationException(
            node,
            YamlActionGroup::class.java,
            "Could not load group ${actionGroup.name}: malformed yml."
        )
    }

    private fun scanFileForGroups(file: Path): List<YamlActionGroup> {
        val groups = mutableListOf<YamlActionGroup>()
        val node = YamlConfigurationLoader.builder()
            .path(file).build().load()
        for (key in node.childrenMap().keys) {
            groups.add(YamlActionGroup(key.toString(), emptyList(), emptyList(), emptyMap()))
        }
        return groups
    }

}