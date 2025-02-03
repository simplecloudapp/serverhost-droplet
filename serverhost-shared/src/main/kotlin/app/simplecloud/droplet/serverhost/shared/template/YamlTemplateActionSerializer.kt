package app.simplecloud.droplet.serverhost.shared.template

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes
import com.google.protobuf.TextFormat.Printer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type


class YamlTemplateActionSerializer : TypeSerializer<YamlTemplateActionsMap> {
    
    private fun nonVirtualNode(source: ConfigurationNode, vararg path: Any): ConfigurationNode {
        if (!source.hasChild(*path)) {
            throw SerializationException("Required field " + path.contentToString() + " was not present in node")
        }
        return source.node(*path)
    }

    override fun deserialize(type: Type?, node: ConfigurationNode?): YamlTemplateActionsMap {
        return InferredYamlTemplateActionsMap(node?.childrenMap()?.map { triggerType ->
            YamlActionTriggerTypes.valueOf(
                triggerType.key.toString().uppercase()
            ) to nonVirtualNode(node, triggerType.key.toString()).childrenList().map { it.string ?: "" }
        }?.toMap() ?: mapOf())
    }

    override fun serialize(type: Type?, obj: YamlTemplateActionsMap?, node: ConfigurationNode) {
        if (obj == null) return
        obj.forEach { (key, value) ->
            node.node(key).setList(String::class.java, value.toList())
        }
    }
}