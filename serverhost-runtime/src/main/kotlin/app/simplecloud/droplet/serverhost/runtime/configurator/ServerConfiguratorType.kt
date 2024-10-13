package app.simplecloud.droplet.serverhost.runtime.configurator

import app.simplecloud.droplet.serverhost.runtime.configurator.impl.*

enum class ServerConfiguratorType(val configurator: ServerConfigurator<*>) {
    YML(YamlServerConfigurator),
    PROPERTIES(PropertiesServerConfigurator),
    TOML(TomlServerConfigurator),
    JSON(JsonServerConfigurator),
    TXT(TextServerConfigurator);
}