package app.simplecloud.droplet.serverhost.shared.actions.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.shared.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext

object InferFromServerAction : YamlAction<InferFromServerActionData> {
    override fun exec(ctx: YamlActionContext, data: InferFromServerActionData) {
        val server =
            ctx.retrieve<Server>("server") ?: throw NullPointerException("server is not present in action context")
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholders are not present in action context")
        if (data.field.startsWith("$,")) {
            if(!server.properties.containsKey(data.field.substring(2))) {
                return
            }
            placeholders.set(data.key, server.properties.getOrDefault(data.field.substring(2), ""))
            placeholders.save(ctx)
            return
        }
        val field = server.javaClass.getField(data.field)
        if (!field.canAccess(server)) {
            return
        }
        placeholders.set(data.key, field.get(server).toString())
        placeholders.save(ctx)
    }

    override fun getDataType(): Class<InferFromServerActionData> {
        return InferFromServerActionData::class.java
    }
}