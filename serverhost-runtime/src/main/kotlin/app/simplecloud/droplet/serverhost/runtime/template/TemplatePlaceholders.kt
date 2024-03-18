package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.controller.shared.server.Server

class TemplatePlaceholders {
    companion object {
        private const val SERVER = "%SERVER%"
        private const val NUMERICAL_ID = "%NUMERICAL_ID%"
        private const val GROUP = "%GROUP%"

        fun parse(content: String, context: Server): String {
            return content
                .replace(SERVER, "${context.group}-${context.numericalId}")
                .replace(NUMERICAL_ID, "${context.numericalId}")
                .replace(GROUP, context.group)
        }
    }
}