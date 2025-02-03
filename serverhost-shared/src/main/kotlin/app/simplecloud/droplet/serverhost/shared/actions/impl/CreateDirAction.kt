package app.simplecloud.droplet.serverhost.shared.actions.impl

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists

object CreateDirAction : YamlAction<CreateDirActionData> {
    override fun exec(ctx: YamlActionContext, data: CreateDirActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholder context is required but was not found")
        val dir = Paths.get(placeholders.parse(data.dir))
        if(!dir.exists())
            Files.createDirectories(dir)
    }

    override fun getDataType(): Class<CreateDirActionData> {
        return CreateDirActionData::class.java
    }
}