package app.simplecloud.droplet.serverhost.shared.actions.impl.execute

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import java.nio.file.Paths

object ExecuteAction : YamlAction<ExecuteActionData> {
    override fun exec(ctx: YamlActionContext, data: ExecuteActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholder context is required but was not found")
        val builder = ProcessBuilder(placeholders.parse(data.command).split(" "))
        if (data.workingDirectory != null) {
            builder.directory(Paths.get(placeholders.parse(data.workingDirectory)).toFile())
        }
        if (data.environment != null) {
            builder.environment().putAll(data.environment.mapValues { placeholders.parse(it.value) })
        }
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        builder.redirectError(ProcessBuilder.Redirect.INHERIT)
        builder.start().waitFor()
    }

    override fun getDataType(): Class<ExecuteActionData> {
        return ExecuteActionData::class.java
    }
}