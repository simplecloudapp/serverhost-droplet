package app.simplecloud.droplet.serverhost.shared.actions.impl.env

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext

object EnvAction : YamlAction<EnvActionData> {
    override fun exec(ctx: YamlActionContext, data: EnvActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx) ?: throw NullPointerException("placeholder context is required but was not found")
        val env = ctx.retrieve<MutableMap<String, String>>("env") ?: mutableMapOf()
        val value = placeholders.parse(data.value)
        env[data.key] = value
        ctx.store("env", env)
    }

    override fun getDataType(): Class<EnvActionData> {
        return EnvActionData::class.java
    }
}