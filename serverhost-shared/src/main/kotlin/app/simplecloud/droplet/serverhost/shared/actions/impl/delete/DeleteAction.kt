package app.simplecloud.droplet.serverhost.shared.actions.impl.delete

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import org.apache.commons.io.FileUtils
import java.nio.file.Path
import kotlin.io.path.exists

object DeleteAction : YamlAction<DeleteActionData> {
    override fun exec(ctx: YamlActionContext, data: DeleteActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholder context is required but was not found")
        val path = Path.of(placeholders.parse(data.path))
        if (!path.exists()) return
        if (data.force) {
            FileUtils.forceDelete(path.toFile())
            return
        }
        FileUtils.delete(path.toFile())
    }

    override fun getDataType(): Class<DeleteActionData> {
        return DeleteActionData::class.java
    }
}