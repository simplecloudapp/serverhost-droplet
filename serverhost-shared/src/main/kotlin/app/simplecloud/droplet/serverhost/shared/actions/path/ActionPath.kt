package app.simplecloud.droplet.serverhost.shared.actions.path

import java.nio.file.Path

class ActionPath(
    private val stringPath: String,
) {
    fun asPath(templates: Map<String, Any>): Path {
        var templatedPath = stringPath
        templates.forEach { (s, any) ->
            templatedPath =
                templatedPath.replace(
                    "%${
                        s.lowercase()
                            .replace(" ", "-")
                            .replace("_", "-")
                    }%", any.toString()
                )
        }
        return Path.of(templatedPath)
    }

    fun asPath(): Path {
        return Path.of(stringPath)
    }
}
