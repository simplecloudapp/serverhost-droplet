package app.simplecloud.droplet.serverhost.runtime.process

import app.simplecloud.droplet.serverhost.runtime.util.ProcessDirectory
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicReference
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
        // Check the current handle first
        val currentCommand = handle.info().command().orElse("").lowercase()
        if (currentCommand.contains(executable.lowercase())) {
            return Optional.of(handle)
        }

        val found = AtomicReference<Optional<ProcessHandle>>(Optional.empty())

        // Check immediate children
        handle.children().forEach { child ->
            if (found.get().isPresent) return@forEach

            val command = child.info().command().orElse("").lowercase()

            if (command.contains(executable.lowercase())) {
                found.set(Optional.of(child))
            }
        }

        // Return if we found a match in the first pass
        if (found.get().isPresent) {
            return found.get()
        }

        // Recursive search through children
        handle.children().forEach { child ->
            if (found.get().isPresent) return@forEach

            val childResult = findHighestExecutableProcess(child, executable)
            if (childResult.isPresent) {
                found.set(childResult)
            }
        }

        return found.get()
    }

}