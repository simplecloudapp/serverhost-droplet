package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.ResourcePath
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class YamlActionLoaderTest {

    private val directory = ResourcePath.get("actions")

    @Test
    fun testFileDeserialize() {
        assertEquals(LoadResults.FILE_LOAD_RESULT.toSet(), YamlActionLoader.loadActionFiles(directory).toSet())
    }

    @Test
    fun testGroupDeserialize() {
        val files = YamlActionLoader.loadActionFiles(directory)
        val result = YamlActionLoader.loadActionGroups(directory, files, mutableMapOf())
        assertEquals(LoadResults.GROUP_LOAD_RESULT.toSet(), result.toSet())
    }

    @Test
    fun testRefDeserialize() {
        val refMap = mutableMapOf<String, List<String>>()
        val files = YamlActionLoader.loadActionFiles(directory)
        YamlActionLoader.loadActionGroups(directory, files, refMap)
        assertEquals(
            refMap, mapOf(
                "cache/cache-pull" to listOf(),
                "cache/cache-spigot" to listOf(),
                "backup/weekly" to listOf(),
                "cache/cache-paper" to listOf("cache/cache-spigot")
            )
        )
    }

    @Test
    fun testRefLoadOrder() {
        val refMap = mutableMapOf<String, List<String>>()
        val files = YamlActionLoader.loadActionFiles(directory)
        YamlActionLoader.loadActionGroups(directory, files, refMap)
        val resolvedRefs = YamlActionLoader.resolveRefTree(refMap)
        var resolvedDependency = false
        resolvedRefs.forEach {
            if (!resolvedDependency && (it == "cache/cache-paper")) fail("cache/cache-spigot was not loaded before cache/cache-paper")
            if (it == "cache/cache-spigot") resolvedDependency = true
        }
        assert(resolvedDependency)
    }

    @Test
    fun testConstruction() {
        val refMap = mutableMapOf<String, List<String>>()
        var files = YamlActionLoader.loadActionFiles(directory)
        files = YamlActionLoader.loadActionGroups(directory, files, refMap)
        val resolvedRefs = YamlActionLoader.resolveRefTree(refMap)
        assert(YamlActionLoader.constructActions(resolvedRefs, files)["cache/cache-paper"]?.size == 3)
    }

    @Test
    fun testLoad() {
        assert(YamlActionLoader.load(directory)["cache/cache-paper"]?.size == 3)
    }
}