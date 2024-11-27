package app.simplecloud.droplet.serverhost.shared.actions.impl

import app.simplecloud.droplet.serverhost.shared.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object CopyAction : YamlAction<CopyActionData> {

    override fun exec(ctx: YamlActionContext, data: CopyActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholder context is required but was not found")
        val from = Paths.get(placeholders.parse(data.from))
        if (!from.exists()) {
            throw NullPointerException("from file does not exist")
        }
        val to = Paths.get(placeholders.parse(data.to))
        if(to.exists() && !data.replace) {
            return
        }
        if (!to.exists()) {
            Files.createParentDirs(to.toFile())
            if(to.isDirectory())
                to.toFile().mkdirs()
        }
        if (from.isDirectory()) {
            FileUtils.copyDirectory(from.toFile(), to.toFile())
            return
        }
        FileUtils.copyFile(from.toFile(), to.toFile())

    }

    override fun getDataType(): Class<CopyActionData> {
        return CopyActionData::class.java
    }
}