package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.actions.impl.CopyAction
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.reflect.KClass

class YamlActionLoader {

    fun load() {
        val actionFiles = loadFiles(Paths.get("actions"))
    }

    fun loadFiles(directory: Path): List<YamlActionFile> {
        if(!directory.isDirectory()) throw IllegalArgumentException("Not a directory: $directory")
        val actionFiles = mutableListOf<YamlActionFile>()
        for (entry in directory.listDirectoryEntries("*.yml")) {
            val fileName = entry.nameWithoutExtension
            val groups = scanFileForGroups(entry)
            actionFiles.add(YamlActionFile(fileName, groups))
        }
        return actionFiles
    }

    private fun scanFileForGroups(file: Path): List<YamlActionGroup> {
        val groups = mutableListOf<YamlActionGroup>()
        val node = YamlConfigurationLoader.builder()
            .path(file).build().load()
        for (key in node.childrenMap().keys) {
            groups.add(YamlActionGroup(key.toString(), emptyList()))
        }
        return groups
    }

}