package app.simplecloud.droplet.serverhost.shared.actions.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import kotlin.reflect.full.memberProperties

object InferFromServerAction : YamlAction<InferFromServerActionData> {
    override fun exec(ctx: YamlActionContext, data: InferFromServerActionData) {
        val server =
            ctx.retrieve<Server>("server") ?: throw NullPointerException("server is not present in action context")
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholders are not present in action context")
        if (data.field.startsWith("$.")) {
            if (!server.properties.containsKey(data.field.substring(2))) {
                return
            }
            var value = server.properties.getOrDefault(data.field.substring(2), "")
            if(data.lowercase) value = value.lowercase()
            placeholders.set(data.key, value)
            placeholders.save(ctx)
            return
        }
        try {
            val field = Server::class.memberProperties.firstOrNull { it.name == data.field } ?: return
            var value = field.get(server).toString()
            if(data.lowercase) {
                value = value.lowercase()
            }
            placeholders.set(data.key, value)
            placeholders.save(ctx)
        }catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun getDataType(): Class<InferFromServerActionData> {
        return InferFromServerActionData::class.java
    }
}