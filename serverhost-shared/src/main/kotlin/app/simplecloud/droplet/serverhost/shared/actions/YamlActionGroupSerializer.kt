package app.simplecloud.droplet.serverhost.shared.actions

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class YamlActionGroupSerializer(
    private val actionFiles: List<YamlActionFile>,
    private val currentFile: YamlActionFile,
    private val currentGroup: YamlActionGroup,
    private val refTree: MutableMap<String, List<String>>
) : TypeSerializer<YamlActionGroup> {

    private val dataSerializer = YamlActionDataSerializer()
    //The reference to the current group
    private val selfRef = "${currentFile.fileName}/${currentGroup.name}"

    private fun nonVirtualNode(source: ConfigurationNode, vararg path: Any): ConfigurationNode {
        if (!source.hasChild(*path)) {
            throw SerializationException("Required field " + path.contentToString() + " was not present in node")
        }
        return source.node(*path)
    }

    override fun deserialize(type: Type?, node: ConfigurationNode?): YamlActionGroup? {
        if (node == null) return null

        refTree[selfRef] = listOf()
        var refIndex = 0;
        var dataIndex = 0;
        var flowIndex = 0;
        val refList = mutableListOf<String>()
        val dataList = mutableListOf<YamlActionData>()
        val flowList = mutableMapOf<Int, Pair<YamlActionDataDescriptor, Int>>()
        node.childrenList().forEach { child ->
            if (child.hasChild("ref")) {
                val ref = nonVirtualNode(child, "ref").string ?: return@forEach
                val foundRef = findRef(ref) ?: throw SerializationException(
                    child.node("ref"),
                    String::class.java,
                    "The reference $ref does not point to any group"
                )
                refList.add("${foundRef.first.fileName}/${foundRef.second.name}")
                val existingRefs = refTree.getOrDefault(selfRef, listOf())
                val combinedRefs = mutableListOf<String>()
                combinedRefs.addAll(existingRefs)
                combinedRefs.add("${foundRef.first.fileName}/${foundRef.second.name}")
                refTree[selfRef] = combinedRefs
                flowList[flowIndex] = Pair(YamlActionDataDescriptor.REF, refIndex)
                refIndex++
                flowIndex++
                return@forEach
            }
            if (child.hasChild("type")) {
                val data = dataSerializer.deserialize(YamlActionData::class.java, child) ?: return@forEach
                dataList.add(data)
                flowList[flowIndex] = Pair(YamlActionDataDescriptor.DATA, dataIndex)
                dataIndex++
            }
            flowIndex++
        }

        return YamlActionGroup(currentGroup.name, dataList, refList, flowList)
    }

    override fun serialize(type: Type?, obj: YamlActionGroup?, node: ConfigurationNode) {
        if (obj == null) return
        for (flowKey in obj.actionFlowList.keys.sorted()) {
            val childNode = node.appendListNode()
            val flowElement = obj.actionFlowList[flowKey] ?: continue
            when (flowElement.first) {
                YamlActionDataDescriptor.REF -> childNode.node("ref")
                    .set(obj.actionRefList.elementAt(flowElement.second))

                YamlActionDataDescriptor.DATA -> {
                    val element = obj.actionDataList.elementAt(flowElement.second)
                    dataSerializer.serialize(YamlActionData::class.java, element, childNode)
                }
            }
        }
    }

    //Resolve a reference by a ref string
    private fun findRef(ref: String): Pair<YamlActionFile, YamlActionGroup>? {
        if (!ref.contains("/")) return currentFile.groups.firstOrNull { it.name == ref }?.let { Pair(currentFile, it) }
        val args = ref.split("/")
        val fileName = args[0]
        val groupName = args[1]
        val file = actionFiles.firstOrNull { it.fileName == fileName } ?: return null
        return file.groups.firstOrNull { it.name == groupName }?.let { Pair(file, it) }
    }

}