package app.simplecloud.droplet.serverhost.shared.actions

import java.util.SortedMap

data class YamlActionGroup(
    val name: String,
    val actionDataList: List<YamlActionData>,
    //The string represents the ref as a string, the value is the group element the ref points to
    val actionRefMap: SortedMap<String, YamlActionGroup>,
    // Pair: the type of the data descriptor alongside the index in the corresponding list
    val actionFlowList: List<Pair<YamlActionDataDescriptor, Int>>,
)