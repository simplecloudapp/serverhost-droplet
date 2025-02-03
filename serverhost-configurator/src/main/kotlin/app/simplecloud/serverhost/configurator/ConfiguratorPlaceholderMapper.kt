package app.simplecloud.serverhost.configurator

object ConfiguratorPlaceholderMapper {

    fun map(templatedString: String, configurable: Configurable, forwardingSecret: String): String {
        var result = templatedString
        val templateMap = mutableMapOf<String, Any>()
        templateMap.putAll(configurable.getPlaceholderMappings())
        templateMap["forwarding-secret"] = forwardingSecret
        for ((template, value) in templateMap) {
            result = result.replace("%$template%", value.toString())
        }
        return result
    }
}