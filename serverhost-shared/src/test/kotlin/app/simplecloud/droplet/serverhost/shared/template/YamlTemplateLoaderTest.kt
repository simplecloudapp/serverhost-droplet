package app.simplecloud.droplet.serverhost.shared.template

import app.simplecloud.droplet.serverhost.shared.ResourcePath
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionLoader
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals
import kotlin.test.fail

class YamlTemplateLoaderTest {

    private val templateDir = ResourcePath.get("templates")
    private val actionDir = ResourcePath.get("actions")

    @Test
    fun load() {
        val actions = YamlActionLoader.load(actionDir)
        val templates = YamlTemplateLoader.load(templateDir, actions)
        if(templates.first.size != 1) fail()
        if(templates.second.size != 3) fail()

        val testTemplate = templates.first[0]
        assertEquals(testTemplate.name, "test")
        assertEquals(testTemplate.actionMap.entries, InferredYamlTemplateActionsMap(mapOf(YamlActionTriggerTypes.START to listOf("cache/cache-pull"))).entries)
    }
}