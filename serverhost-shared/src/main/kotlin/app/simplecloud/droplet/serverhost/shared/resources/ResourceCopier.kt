package app.simplecloud.droplet.serverhost.shared.resources

import java.io.File
import java.net.URI
import java.nio.file.*
import kotlin.io.path.*


class ResourceCopier {

    @OptIn(ExperimentalPathApi::class)
    fun copyAll(dir: String, overwriteRule: (path: Path) -> Boolean = { true }) {
        val url = ResourceCopier::class.java.getResource("/$dir") ?: return
        val uri = url.toURI()
        val fileSystem = if (uri.scheme == "jar") createFileSystem(uri) else null
        val path = Paths.get(uri)
        if (!path.exists()) return
        if (!path.isDirectory()) return
        path.listDirectoryEntries().forEach {
            it.walk(PathWalkOption.BREADTH_FIRST).forEach { child ->
                val relative = File(child.relativeTo(path).pathString).toPath()
                if (relative.parent == null || !relative.parent.exists())
                    Files.createDirectories(relative)
                val opts = if (overwriteRule(relative)) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray();
                Files.copy(child, relative, *opts)
            }
        }
        fileSystem?.close()
    }

    private fun createFileSystem(uri: URI): FileSystem {
        val env: MutableMap<String, String> = HashMap()
        env["create"] = "true"
        env["encoding"] = "UTF-8"
        return FileSystems.newFileSystem(uri, env)
    }

}