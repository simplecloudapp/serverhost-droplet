package app.simplecloud.droplet.serverhost.shared.actions.impl.copy

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

object CopyAction : YamlAction<CopyActionData> {

    override fun exec(ctx: YamlActionContext, data: CopyActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("placeholder context is required but was not found")
        val from = Paths.get(placeholders.parse(data.from))
        if (!from.exists()) {
            if (data.initDirIfMissing) {
                Files.createParentDirs(from.toFile())
                from.toFile().mkdirs()
                return
            }
            throw NullPointerException("from file does not exist ($from)")
        }
        val to = Paths.get(placeholders.parse(data.to))
        if (to.exists() && !to.isDirectory() && !data.replace) {
            return
        }
        if (!to.exists()) {
            Files.createParentDirs(to.toFile())
            if (to.isDirectory() || to.name == to.nameWithoutExtension)
                to.toFile().mkdirs()
        }
        if (from.isDirectory()) {
            if (!data.replace)
                FileUtils.copyDirectory(from.toFile(), to.toFile()) { f ->
                    !from.resolve(to.relativize(f.toPath())).exists()
                }
            else
                FileUtils.copyDirectory(from.toFile(), to.toFile())
            return
        }
        if (to.isDirectory() || to.name == to.nameWithoutExtension)
            FileUtils.copyToDirectory(from.toFile(), to.toFile())
        else
            FileUtils.copyFile(from.toFile(), to.toFile(), StandardCopyOption.REPLACE_EXISTING)
    }

    override fun getDataType(): Class<CopyActionData> {
        return CopyActionData::class.java
    }
}