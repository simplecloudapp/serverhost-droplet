package app.simplecloud.droplet.serverhost.runtime.util

import app.simplecloud.droplet.serverhost.shared.hack.OS
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

object ProcessDirectory {

    fun of(handle: ProcessHandle): Optional<Path> {
        val os = OS.get() ?: return Optional.empty()
        return when (os) {
            OS.LINUX, OS.MAC -> {
                ofUnix(handle.pid())
            }
            OS.WINDOWS -> {
                //TODO: Add native code (or leave as is, will still work on windows)
                Optional.empty()
            }
        }
    }

    private fun ofUnix(pid: Long): Optional<Path> {
        val path = Paths.get("/proc", pid.toString(), "cwd")
        return try {
            Optional.of(Files.readSymbolicLink(path))
        }catch (e: Exception) {
            Optional.empty()
        }
    }
}