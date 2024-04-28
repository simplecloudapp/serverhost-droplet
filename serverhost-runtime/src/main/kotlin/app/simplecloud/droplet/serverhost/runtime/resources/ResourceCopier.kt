package app.simplecloud.droplet.serverhost.runtime.resources

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

class ResourceCopier {

    @OptIn(ExperimentalPathApi::class)
    fun copyAll(dir: String) {
        val url = ResourceCopier::class.java.getResource("/$dir") ?: return

        val path = url.toURI().toPath()
        if(!path.exists()) return
        if(!path.isDirectory()) return
        path.listDirectoryEntries().forEach {
            it.walk(PathWalkOption.BREADTH_FIRST).forEach { child ->
                val relative = child.relativeTo(path)
                if(!relative.parent.exists())
                    Files.createDirectories(relative)
                Files.copy(child, relative, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

}