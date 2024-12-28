package app.simplecloud.droplet.serverhost.runtime.process

import app.simplecloud.droplet.serverhost.runtime.util.ProcessDirectory
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString

object ProcessFinder {

    fun findHighestProcessWorkingDir(
        dir: Path,
        handle: ProcessHandle,
        executable: String,
    ): Optional<ProcessHandle> {
        val path = ProcessDirectory.of(handle).orElse(dir)
        if (path.normalize().absolutePathString() == dir.normalize().absolutePathString() && ProcessInfo.of(handle)
                .getCommand().lowercase().startsWith(executable)
        ) {
            return Optional.of(handle)
        }
        val parent = handle.parent().orElse(null) ?: return Optional.empty()
        return findHighestProcessWorkingDir(dir, parent, executable)
    }

    fun findHighestExecutableProcess(handle: ProcessHandle, executable: String): Optional<ProcessHandle> {
        var returned = Optional.empty<ProcessHandle>()
        handle.children().forEach { child ->
            if (!returned.isEmpty) return@forEach
            if (ProcessInfo.of(child).getCommand().lowercase().startsWith(executable)) {
                returned = Optional.of(child)
            }
        }
        if (!returned.isEmpty) return returned
        handle.children().forEach { child ->
            if (!returned.isEmpty) return@forEach
            returned = findHighestExecutableProcess(child, executable)
        }
        return returned
    }

}