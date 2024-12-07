package app.simplecloud.droplet.serverhost.shared.actions

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

    fun setRunning(path: Path) {
        set("running", path)
    }

    fun setServerDir(path: Path) {
        set("server-dir", path)
    }

    fun setTemplate(path: Path) {
        set("templates", path)
    }

    fun setLibs(path: Path) {
        set("libs-dir", path)
    }

    fun setGroup(group: String) {
        set("group", group)
    }

    fun save(context: YamlActionContext) {
        context.store("placeholders", placeholders)
        if(placeholders.containsKey("server-dir")) {
            context.store("server-dir", placeholders["server-dir"]!!)
        }
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
            val serverDir = context.retrieve<String>("server-dir")
            if(serverDir != null)
                map["server-dir"] = serverDir
            return YamlActionPlaceholderContext(map)
        }
    }
}