package app.simplecloud.droplet.serverhost.shared.actions

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class YamlActionSerializer(
    private val actionFiles: List<YamlActionFile>
): TypeSerializer<List<YamlActionData>> {

    private fun nonVirtualNode(source: ConfigurationNode, vararg path: Any): ConfigurationNode {
        if (!source.hasChild(*path)) {
            throw SerializationException("Required field " + path.contentToString() + " was not present in node")
        }
        return source.node(*path)
    }

    override fun deserialize(type: Type?, node: ConfigurationNode?): List<YamlActionData> {
        val returned = mutableListOf<YamlActionData>()
        if(node == null) return returned
        if(!node.hasChild("ref")) {
            val type = YamlActionTypes.valueOf(nonVirtualNode(node, "type").string!!.uppercase())
            val data = node.childrenMap()

            return listOf();
        }
        return listOf()
    }


    override fun serialize(type: Type?, obj: List<YamlActionData>?, node: ConfigurationNode?) {
        TODO("Not yet implemented")
    }
}