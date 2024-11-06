package app.simplecloud.droplet.serverhost.shared.actions

import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals

class YamlActionLoaderTest {

    private val loader = YamlActionLoader()
    private val directory = Paths.get("actions")

    @Test
    fun testFileLoad() {
        assertEquals(LoadResults.FILE_LOAD_RESULT, loader.loadActionFiles(directory))
    }

    @Test
    fun testGroupLoad() {
        val files = loader.loadActionFiles(directory)
        val result = loader.loadActionGroups(directory, files)
        assertEquals(LoadResults.GROUP_LOAD_RESULT, result)
    }
}