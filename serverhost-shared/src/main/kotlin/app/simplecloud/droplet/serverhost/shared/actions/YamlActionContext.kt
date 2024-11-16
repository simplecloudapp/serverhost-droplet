package app.simplecloud.droplet.serverhost.shared.actions

class YamlActionContext {

    private val data = mutableMapOf<String, Any>()

    fun store(key: String, data: Any) {
        this.data[key] = data
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> retrieve(key: String): T? {
        return this.data.getOrDefault(key, null) as T?
    }
}