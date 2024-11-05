package app.simplecloud.droplet.serverhost.runtime.config

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.serverhost.config.YamlConfig

open class YamlGroupConfig(dirPath: String) : YamlConfig(dirPath) {

    inline fun <reified T> load(group: Group): T? {
        return load<T>(group.name + ".yml")
    }

    override fun <T> save(path: String?, obj: T) {
        super.save("$path.yml", obj)
    }

    fun <T> save(group: Group, obj: T) {
        save(group.name + ".yml", obj)
    }
}