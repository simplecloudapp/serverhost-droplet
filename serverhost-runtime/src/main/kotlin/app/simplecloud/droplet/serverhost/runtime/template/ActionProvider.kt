package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionLoader
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTypes
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import kotlin.io.path.isDirectory

class ActionProvider(private val dir: Path) {

    private val logger = LogManager.getLogger(TemplateProvider::class.java)
    private var actions = mapOf<String, List<Pair<YamlActionTypes, Any>>>()

    fun load() {
        if (!dir.isDirectory()) {
            logger.error("Action directory is not a directory")
            return
        }
        try {
            actions = YamlActionLoader.load(dir)
        }catch (e: Exception) {
            logger.error(e.message)
        }
        logger.info("Loaded ${actions.size} actions")
    }

    fun getLoadedActions(): Map<String, List<Pair<YamlActionTypes, Any>>> {
        return actions
    }

}