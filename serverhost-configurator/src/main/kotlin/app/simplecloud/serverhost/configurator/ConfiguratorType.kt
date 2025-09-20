package app.simplecloud.serverhost.configurator

import app.simplecloud.serverhost.configurator.impl.*

enum class ConfiguratorType(val configurator: Configurator<*>) {
    YML(YamlConfigurator),
    PROPERTIES(PropertiesConfigurator),
    TOML(TomlConfigurator),
    HOCON(HoconConfigurator),
    JSON(JsonConfigurator),
    TXT(TextConfigurator);
}