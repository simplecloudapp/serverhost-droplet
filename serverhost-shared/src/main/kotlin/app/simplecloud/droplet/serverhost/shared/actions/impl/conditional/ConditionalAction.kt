package app.simplecloud.droplet.serverhost.shared.actions.impl.conditional

import app.simplecloud.droplet.serverhost.shared.actions.*

object ConditionalAction : YamlAction<ConditionalActionData> {
    override fun exec(ctx: YamlActionContext, data: ConditionalActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholder context is required but was not found")
        val actions = ctx.retrieve<Map<String, List<Pair<YamlActionTypes, Any>>>>("actions") ?: throw NullPointerException("actions context is required but was not found")
        val parsedFirst = placeholders.parse(data.first)
        val parsedSecond = placeholders.parse(data.second)
        if(!ConditionalActionMatchers.valueOf(data.matcher.uppercase()).matcher.match(parsedFirst, parsedSecond)) throw Exception("Assertation ${data.matcher} failed: $parsedFirst, $parsedSecond")
        if(YamlActionGroupExecutor(ctx, actions).execute(data.executes).isNotEmpty()) {
            throw Exception("action ${data.executes} does not exist or finished with errors.")
        }
    }

    override fun getDataType(): Class<ConditionalActionData> {
        return ConditionalActionData::class.java
    }
}