package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString

object TemplatePlaceholders {
    private const val SERVER = "%SERVER%"
    private const val NUMERICAL_ID = "%NUMERICAL_ID%"
    private const val GROUP = "%GROUP%"

    fun parsePath(path: Path, prefix: Path): Path {
        if (path.startsWith("/")) return path
        Paths.get(prefix.pathString, path.pathString)
        return Paths.get(prefix.pathString, path.pathString)
    }

    fun parsePath(path: String, prefix: String): String {
        if (path.startsWith("/")) return path
        return "$prefix/$path"
    }

    fun parse(content: String, context: Server): Path {
        return Paths.get(content
            .replace(SERVER, "${context.group}-${context.numericalId}")
            .replace(NUMERICAL_ID, "${context.numericalId}")
            .replace(GROUP, context.group))
    }
}