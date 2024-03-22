package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server

object TemplatePlaceholders {
    private const val SERVER = "%SERVER%"
    private const val NUMERICAL_ID = "%NUMERICAL_ID%"
    private const val GROUP = "%GROUP%"
    val TEMPLATE_PATH = System.getenv("TEMPLATE_PATH") ?: "templates"

    fun parsePath(path: String, prefix: String): String {
        if(path.startsWith("/")) return path
        return "$prefix/$path"
    }

    fun parse(content: String, context: Server): String {
        return content
            .replace(SERVER, "${context.group}-${context.numericalId}")
            .replace(NUMERICAL_ID, "${context.numericalId}")
            .replace(GROUP, context.group)
    }
}