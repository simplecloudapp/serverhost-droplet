package app.simplecloud.droplet.serverhost.runtime.hack

enum class OS(val names: List<String>) {
    WINDOWS(listOf("windows")),
    UNIX(listOf("mac", "linux"));

    companion object {
        fun get(): OS? {
            val name = System.getProperty("os.name").lowercase()
            OS.values().forEach {
                if (it.names.any { osName -> name.contains(osName) }) {
                    return it
                }
            }
            return null
        }
    }
}