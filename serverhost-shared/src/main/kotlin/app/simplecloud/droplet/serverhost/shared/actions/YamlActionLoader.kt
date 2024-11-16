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

object YamlActionLoader {


    fun load(directory: Path): Map<String, List<Pair<YamlActionTypes, Any>>> {
        val refTree = mutableMapOf<String, List<String>>()
        var actionFiles = loadActionFiles(directory)
        //This will now deserialize all the groups that were loaded and update the list
        actionFiles = loadActionGroups(directory, actionFiles, refTree)
        val loadOrder = resolveRefTree(refTree)
        return constructActions(loadOrder, actionFiles)
    }

    //Constructs actions into a format that is easier to deal with for execution
    fun constructActions(
        loadOrder: List<String>,
        actionFiles: List<YamlActionFile>
    ): Map<String, List<Pair<YamlActionTypes, Any>>> {
        val result = mutableMapOf<String, List<Pair<YamlActionTypes, Any>>>()
        for (ref in loadOrder) {
            val actions = mutableListOf<Pair<YamlActionTypes, Any>>()
            val args = ref.split("/")
            val fileName = args[0]
            val groupName = args[1]
            val file = actionFiles.firstOrNull { it.fileName == fileName } ?: continue
            val group = file.groups.firstOrNull { it.name == groupName } ?: continue
            val flowEntries = group.actionFlowList.toSortedMap()
            for (flowEntry in flowEntries) {
                when (flowEntry.value.first) {
                    YamlActionDataDescriptor.DATA -> actions.add(
                        Pair(
                            group.actionDataList[flowEntry.value.second].type,
                            group.actionDataList[flowEntry.value.second].data
                        )
                    )

                    YamlActionDataDescriptor.REF -> actions.addAll(
                        result.getOrDefault(
                            group.actionRefList[flowEntry.value.second],
                            listOf()
                        )
                    )
                }
            }
            result[ref] = actions
        }
        return result
    }

    //Resolves the ref tree and creates a load order by it. If no load order can be created (circular refs) an error is thrown.
    fun resolveRefTree(refTree: MutableMap<String, List<String>>): List<String> {
        val loadOrder = mutableListOf<String>()
        val visited = mutableMapOf<String, Boolean>()  // true means fully processed, false means processing
        val processingStack = mutableSetOf<String>()
        // Perform DFS for each node to build load order
        for (node in refTree.keys) {
            if (visited[node] != true && dfs(refTree, visited, node, processingStack, loadOrder)) {
                throw IllegalArgumentException("Cycle detected: Cannot determine load order due to circular dependencies.")
            }
        }

        // Reverse load order for correct order of loading
        return loadOrder
    }

    //This deserializes all groups
    fun loadActionGroups(
        directory: Path,
        actionFiles: List<YamlActionFile>,
        refTree: MutableMap<String, List<String>>
    ): List<YamlActionFile> {
        val returned = mutableListOf<YamlActionFile>()
        walkActionFiles(directory) { entry ->
            val actionFile =
                actionFiles.firstOrNull { it.fileName == entry.nameWithoutExtension } ?: return@walkActionFiles
            val serializedGroups = mutableListOf<YamlActionGroup>()
            for (actionGroup in actionFile.groups) {
                serializedGroups.add(deserializeGroup(entry, actionFiles, actionFile, actionGroup, refTree))
            }
            returned.add(YamlActionFile(actionFile.fileName, serializedGroups))
        }
        return returned
    }

    //This only deserializes the "skeleton" of actions
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

    //This deserializes one action group
    private fun deserializeGroup(
        file: Path,
        actionFiles: List<YamlActionFile>,
        actionFile: YamlActionFile,
        actionGroup: YamlActionGroup,
        refTree: MutableMap<String, List<String>>
    ): YamlActionGroup {
        val loader = YamlConfigurationLoader.builder().path(file).defaultOptions { opts ->
            opts.serializers { serializers ->
                serializers.registerAll(
                    TypeSerializerCollection.builder()
                        .register(
                            YamlActionGroup::class.java,
                            YamlActionGroupSerializer(
                                actionFiles,
                                actionFile,
                                actionGroup,
                                refTree
                            ) // Register serializer for the current group
                        )
                        .registerAnnotatedObjects(objectMapperFactory()) //Makes Data definitions such as CopyActionData possible
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


    // Determine load order for all groups determined by ref chaining
    private fun dfs(
        refTree: MutableMap<String, List<String>>,
        visited: MutableMap<String, Boolean>,
        node: String,
        processingStack: MutableSet<String>,
        loadOrder: MutableList<String>
    ): Boolean {
        if (node in processingStack) {
            // Cycle detected
            throw IllegalArgumentException("Cycle detected: Cannot determine load order due to circular refs.")
        }
        if (visited[node] == true) return false  // Node already processed

        // Mark node as being processed
        processingStack.add(node)
        visited[node] = false

        // Recur for all dependencies (neighbors)
        for (neighbor in refTree[node] ?: emptyList()) {
            if (visited[neighbor] != true && dfs(refTree, visited, neighbor, processingStack, loadOrder)) return true
        }

        // Mark node as fully processed
        processingStack.remove(node)
        visited[node] = true
        loadOrder.add(node)  // Append to load order once all dependencies are loaded
        return false
    }

}