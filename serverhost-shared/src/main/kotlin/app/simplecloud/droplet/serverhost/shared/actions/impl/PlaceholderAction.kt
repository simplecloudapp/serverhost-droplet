package app.simplecloud.droplet.serverhost.shared.actions.impl

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext

object PlaceholderAction: YamlAction<PlaceHolderActionData> {
    override fun exec(ctx: YamlActionContext, data: PlaceHolderActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholder context is required but was not found")
        if(data.key == "server-dir") {
            ctx.store("server-dir", data.value)
            return
        }
        var result = placeholders.parse(data.value)
        if(data.lowercase) result = result.lowercase()
        placeholders.set(data.key, result)
        placeholders.save(ctx)
    }

    override fun getDataType(): Class<PlaceHolderActionData> {
        return PlaceHolderActionData::class.java
    }
}