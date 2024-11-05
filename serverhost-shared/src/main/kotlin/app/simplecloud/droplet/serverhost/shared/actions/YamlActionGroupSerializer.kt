package app.simplecloud.droplet.serverhost.shared.actions

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class YamlActionGroupSerializer(
    private val actionFiles: List<YamlActionFile>,
    private val currentFile: YamlActionFile,
    private val currentGroup: YamlActionGroup
) : TypeSerializer<YamlActionGroup> {

    private val dataSerializer = YamlActionDataSerializer()

    private fun nonVirtualNode(source: ConfigurationNode, vararg path: Any): ConfigurationNode {
        if (!source.hasChild(*path)) {
            throw SerializationException("Required field " + path.contentToString() + " was not present in node")
        }
        return source.node(*path)
    }

    override fun deserialize(type: Type?, node: ConfigurationNode?): YamlActionGroup? {
        if (node == null) return null
        var refIndex = 0;
        var dataIndex = 0;
        val refList = sortedMapOf<String, YamlActionGroup>()
        val dataList = mutableListOf<YamlActionData>()
        val flowList = mutableListOf<Pair<YamlActionDataDescriptor, Int>>()
        node.childrenList().forEach { child ->
            if(child.hasChild("ref")) {
                val ref = nonVirtualNode(child, "ref").string ?: return@forEach
                val refGroup = findRef(ref) ?: throw SerializationException(child.node("ref"), String::class.java, "The reference $ref does not point to any group")
                refList[ref] = refGroup
                flowList.add(Pair(YamlActionDataDescriptor.REF, refIndex))
                refIndex++
                return@forEach
            }
            val data = dataSerializer.deserialize(YamlActionData::class.java, child) ?: return@forEach
            dataList.add(data)
            flowList.add(Pair(YamlActionDataDescriptor.DATA, dataIndex))
            dataIndex++
        }

        return YamlActionGroup(currentGroup.name, dataList, refList, flowList)
    }

    override fun serialize(type: Type?, obj: YamlActionGroup?, node: ConfigurationNode) {
        if (obj == null) return
        for (flowElement in obj.actionFlowList) {
            val childNode = node.appendListNode()
            when(flowElement.first) {
                YamlActionDataDescriptor.REF -> childNode.node("ref").set(obj.actionRefMap.keys.elementAt(flowElement.second))
                YamlActionDataDescriptor.DATA -> {
                    val element = obj.actionDataList.elementAt(flowElement.second)
                    dataSerializer.serialize(YamlActionData::class.java, element, childNode)
                }
            }
        }
    }

    private fun findRef(ref: String): YamlActionGroup? {
        if(!ref.contains("/")) return currentFile.groups.firstOrNull { it.name == ref }
        val args = ref.split("/")
        val fileName = args[0]
        val groupName = args[1]
        return actionFiles.firstOrNull { it.fileName == fileName }?.groups?.firstOrNull { it.name == groupName }
    }

}