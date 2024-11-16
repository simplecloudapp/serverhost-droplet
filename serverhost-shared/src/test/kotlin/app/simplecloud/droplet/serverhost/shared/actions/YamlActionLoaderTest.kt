package app.simplecloud.droplet.serverhost.shared.actions

import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals

class YamlActionLoaderTest {

    private val directory = ResourcePath.get("actions")

    @Test
    fun testFileDeserialize() {
        assertEquals(LoadResults.FILE_LOAD_RESULT, YamlActionLoader.loadActionFiles(directory))
    }

    @Test
    fun testGroupDeserialize() {
        val files = YamlActionLoader.loadActionFiles(directory)
        val result = YamlActionLoader.loadActionGroups(directory, files, mutableMapOf())
        assertEquals(LoadResults.GROUP_LOAD_RESULT, result)
    }

    @Test
    fun testRefDeserialize() {
        val refMap = mutableMapOf<String, List<String>>()
        val files = YamlActionLoader.loadActionFiles(directory)
        YamlActionLoader.loadActionGroups(directory, files, refMap)
        assertEquals(refMap, mapOf(
            "cache/cache-pull" to listOf(),
            "cache/cache-spigot" to listOf(),
            "backup/weekly" to listOf(),
            "cache/cache-paper" to listOf("cache/cache-spigot")
        ))
    }

    @Test
    fun testRefLoadOrder() {
        val refMap = mutableMapOf<String, List<String>>()
        val files = YamlActionLoader.loadActionFiles(directory)
        YamlActionLoader.loadActionGroups(directory, files, refMap)
        val resolvedRefs = YamlActionLoader.resolveRefTree(refMap)
        assert(resolvedRefs.last() == "cache/cache-paper")
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