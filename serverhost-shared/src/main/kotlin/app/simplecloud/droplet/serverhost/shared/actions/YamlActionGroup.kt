package app.simplecloud.droplet.serverhost.shared.actions

data class YamlActionGroup(
    val name: String,
    val actionDataList: List<YamlActionData>,
    //The string represents the ref as a string, the value is the group element the ref points to
    val actionRefList: List<Pair<String, YamlActionGroup>>,
    // Pair: the type of the data descriptor alongside the index in the corresponding list
    val actionFlowList: Map<Int, Pair<YamlActionDataDescriptor, Int>>,
)