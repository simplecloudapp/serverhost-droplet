package app.simplecloud.droplet.serverhost.shared.actions

import java.nio.file.Path
import java.nio.file.Paths

object ResourcePath {
    fun get(fileName: String): Path {
        val resource = this::class.java.classLoader.getResource(fileName)
            ?: throw IllegalArgumentException("File $fileName not found in resources")
        return Paths.get(resource.toURI())
    }
}