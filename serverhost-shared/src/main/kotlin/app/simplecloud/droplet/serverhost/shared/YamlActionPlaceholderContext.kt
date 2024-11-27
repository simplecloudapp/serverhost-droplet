package app.simplecloud.droplet.serverhost.shared

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import java.nio.file.Path

data class YamlActionPlaceholderContext(val placeholders: MutableMap<String, String> = mutableMapOf()) {

    fun set(key: String, value: String) {
        placeholders[key] = value
    }

    fun get(key: String): String? {
        return placeholders[key]
    }

    fun set(key: String, path: Path) {
        placeholders[key] = path.toAbsolutePath().toString()
    }

    fun setServerPath(path: Path) {
        set("server", path)
    }

    fun setTemplate(path: Path) {
        set("template", path)
    }

    fun setLibs(path: Path) {
        set("libs", path)
    }

    fun setGroup(group: String) {
        set("group", group)
    }

    fun save(context: YamlActionContext) {
        context.store("placeholders", placeholders)
    }

    fun parse(templated: String): String {
        var result = templated
        placeholders.forEach { (key, value) ->
            result = result.replace("%${key.lowercase().replace("_", "-").replace(" ", "-")}%", value)
        }
        return result
    }


    companion object {
        fun retrieve(context: YamlActionContext): YamlActionPlaceholderContext? {
            val map = context.retrieve<MutableMap<String, String>>("placeholders") ?: return null
            return YamlActionPlaceholderContext(map)
        }
    }
}