package app.simplecloud.serverhost.configurator

interface Configurable {
    fun getPlaceholderMappings(): Map<String, Any>
}