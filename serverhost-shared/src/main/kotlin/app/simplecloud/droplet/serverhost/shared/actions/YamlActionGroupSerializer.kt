package app.simplecloud.droplet.serverhost.shared.actions

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class YamlActionGroupSerializer(
    private val actionFiles: List<YamlActionFile>,
    private val currentFile: YamlActionFile,
    private val currentGroup: YamlActionGroup,
    private val mode: YamlActionDataDescriptor,
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
        var flowIndex = 0;
        val refList = mutableListOf<Pair<String, YamlActionGroup>>()
        val dataList = mutableListOf<YamlActionData>()
        val flowMap = mutableMapOf<Int, Pair<YamlActionDataDescriptor, Int>>()
        node.childrenList().forEach { child ->
            if(child.hasChild("ref") && mode == YamlActionDataDescriptor.REF) {
                val ref = nonVirtualNode(child, "ref").string ?: return@forEach
                val refGroup = findRef(ref) ?: throw SerializationException(child.node("ref"), String::class.java, "The reference $ref does not point to any group")
                refList.add(Pair(ref, refGroup))
                flowMap[flowIndex] = Pair(YamlActionDataDescriptor.REF, refIndex)
                refIndex++
                return@forEach
            }
            if(child.hasChild("type") && mode == YamlActionDataDescriptor.DATA) {
                val data = dataSerializer.deserialize(YamlActionData::class.java, child) ?: return@forEach
                dataList.add(data)
                flowMap[flowIndex] = Pair(YamlActionDataDescriptor.DATA, dataIndex)
                dataIndex++
            }
            flowIndex++;
        }

        val combinedDataList = mutableListOf<YamlActionData>()
        combinedDataList.addAll(currentGroup.actionDataList)
        combinedDataList.addAll(dataList)

        val combinedRefList = mutableListOf<Pair<String, YamlActionGroup>>()
        combinedRefList.addAll(currentGroup.actionRefList)
        combinedRefList.addAll(refList)

        val combinedFlow = mutableMapOf<Int, Pair<YamlActionDataDescriptor, Int>>()
        combinedFlow.putAll(currentGroup.actionFlowList)
        combinedFlow.putAll(flowMap)

        return YamlActionGroup(currentGroup.name, combinedDataList, combinedRefList, combinedFlow)
    }

    override fun serialize(type: Type?, obj: YamlActionGroup?, node: ConfigurationNode) {
        if (obj == null) return
        for (flowKey in obj.actionFlowList.keys.sorted()) {
            val childNode = node.appendListNode()
            val flowElement = obj.actionFlowList[flowKey] ?: continue
            when(flowElement.first) {
                YamlActionDataDescriptor.REF -> childNode.node("ref").set(obj.actionRefList.elementAt(flowElement.second).first)
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