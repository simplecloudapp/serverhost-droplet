package app.simplecloud.droplet.serverhost.shared.template

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type


class YamlTemplateSerializer : TypeSerializer<YamlTemplateActionMap> {

    private val actionTriggers = YamlActionTriggerTypes.entries.map { it.toString().lowercase() }

    private fun nonVirtualNode(source: ConfigurationNode, vararg path: Any): ConfigurationNode {
        if (!source.hasChild(*path)) {
            throw SerializationException("Required field " + path.contentToString() + " was not present in node")
        }
        return source.node(*path)
    }

    override fun deserialize(type: Type?, node: ConfigurationNode?): YamlTemplateActionMap {
        TODO("Not yet implemented")
    }

    override fun serialize(type: Type?, obj: YamlTemplateActionMap?, node: ConfigurationNode?) {
        TODO("Not yet implemented")
    }
}