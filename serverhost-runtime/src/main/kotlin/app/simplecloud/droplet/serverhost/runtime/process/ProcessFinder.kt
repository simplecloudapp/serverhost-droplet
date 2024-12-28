package app.simplecloud.droplet.serverhost.runtime.process

import app.simplecloud.droplet.serverhost.runtime.util.ProcessDirectory
import java.nio.file.Path
import java.util.*

object ProcessFinder {

    fun findHighestProcessParent(
        serverDir: Path,
        handle: ProcessHandle,
    ): Optional<ProcessHandle> {
        val path = ProcessDirectory.of(handle).orElse(serverDir)
        if (path == serverDir) {
            return Optional.of(handle)
        }
        val parent = handle.parent().orElse(null) ?: return Optional.empty()
        return findHighestProcessParent(serverDir, parent)
    }

    fun findHighestExecutableProcess(handle: ProcessHandle, executable: String): Optional<ProcessHandle> {
        var returned = Optional.empty<ProcessHandle>()
        handle.children().forEach { child ->
            if (!returned.isEmpty) return@forEach
            if (ProcessInfo.of(child).getCommand().startsWith(executable))
                returned = Optional.of(child)
        }
        if (!returned.isEmpty) return returned
        handle.children().forEach { child ->
            if (!returned.isEmpty) return@forEach
            returned = findHighestExecutableProcess(child, executable)
        }
        return returned
    }

}