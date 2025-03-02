package app.simplecloud.droplet.serverhost.shared.actions

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class YamlActionDataSerializer : TypeSerializer<YamlActionData> {

    private fun nonVirtualNode(source: ConfigurationNode, vararg path: Any): ConfigurationNode {
        if (!source.hasChild(*path)) {
            throw SerializationException("Required field " + path.contentToString() + " was not present in node")
        }
        return source.node(*path)
    }

    override fun deserialize(type: Type?, node: ConfigurationNode?): YamlActionData? {
        if (node == null) return null
        val actionType = YamlActionTypes.valueOf(nonVirtualNode(node, "type").string!!.uppercase())
        val actionData = node.get(actionType.action.getDataType()) ?: throw SerializationException(
            node,
            actionType.action.getDataType(),
            "Action of type $actionType was not correctly configured"
        )
        val async = node.node("async").getBoolean(false)
        return YamlActionData(actionType, actionData, async)
    }

    override fun serialize(type: Type?, obj: YamlActionData?, node: ConfigurationNode) {
        if (obj == null) return
        node.set(obj.data)
        node.node("type").set(String::class.java, obj.type.name.lowercase())
        if (obj.async) {
            node.node("async").set(Boolean::class.java, true)
        }
    }

}
