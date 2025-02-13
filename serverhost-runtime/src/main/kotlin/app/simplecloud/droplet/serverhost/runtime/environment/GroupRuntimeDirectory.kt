package app.simplecloud.droplet.serverhost.runtime.environment

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.droplet.serverhost.runtime.config.YamlDirectoryRepository
import java.nio.file.Paths

class GroupRuntimeDirectory :
    YamlDirectoryRepository<GroupRuntime>(Paths.get("options/runtime"), GroupRuntime::class.java) {
        init {
            load()
        }

    fun get(group: String): GroupRuntime? {
        return entities.getOrDefault(entities.keys.find { it.nameWithoutExtension == group }, null)
    }

    fun get(group: Group) : GroupRuntime? {
        return get(group.name)
    }
}