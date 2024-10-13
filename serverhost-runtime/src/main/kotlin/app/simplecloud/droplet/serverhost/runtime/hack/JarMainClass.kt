package app.simplecloud.droplet.serverhost.runtime.hack

import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.name

object JarMainClass {
    fun find(path: Path): String {
        if(!path.name.endsWith(".jar")) return ""
        val jarFile = JarFile(path.toFile())
        val manifest = jarFile.manifest
        val mainClass = manifest.mainAttributes.getValue("Main-Class")
        jarFile.close()
        return mainClass
    }
}