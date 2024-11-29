package app.simplecloud.droplet.serverhost.shared

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionLoader
import app.simplecloud.droplet.serverhost.shared.template.YamlTemplateLoader
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.fail

class DefaultStructureTest {
    private val actionsPath = Path.of("../serverhost-runtime/src/main/resources/copy/templates/actions")
    private val templatesPath = Path.of("../serverhost-runtime/src/main/resources/copy/templates/definitions")

    @Test
    fun load() {
        try {
            val actions = YamlActionLoader.load(actionsPath)
            val result = YamlTemplateLoader.load(templatesPath, actions)
            val templates = result.first
            println("${templates.size} templates loaded")
            val errors = result.second
            errors.forEach { it.printStackTrace() }
            if (errors.isNotEmpty()) {
                fail()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fail()
        }
    }
}