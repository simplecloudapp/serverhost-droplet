package app.simplecloud.droplet.serverhost.shared.actions.impl.placeholder

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext

object PlaceholderAction: YamlAction<PlaceholderActionData> {
    override fun exec(ctx: YamlActionContext, data: PlaceholderActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholder context is required but was not found")
        var result = placeholders.parse(data.value)
        if(data.lowercase) result = result.lowercase()

        if(data.key == "server-dir") {
            ctx.store("server-dir", result)
        }
        placeholders.set(data.key, result)
        placeholders.save(ctx)
    }

    override fun getDataType(): Class<PlaceholderActionData> {
        return PlaceholderActionData::class.java
    }
}