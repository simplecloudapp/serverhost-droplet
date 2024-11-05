package app.simplecloud.droplet.serverhost.shared.actions

import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals

class YamlActionLoaderTest {

    @Test
    fun load() {
        val expected = listOf(
            YamlActionFile(
                "test",
                listOf(
                    YamlActionGroup("test", listOf()),
                    YamlActionGroup("test2", listOf()),
                    YamlActionGroup("test10", listOf())
                )
            ),
            YamlActionFile(
                "test2",
                listOf(
                    YamlActionGroup("test", listOf()),
                    YamlActionGroup("test2", listOf()),
                    YamlActionGroup("test10", listOf())
                )
            )
        )

        val loader = YamlActionLoader()
        val result = loader.loadFiles(Paths.get("actions"))
        assertEquals(expected, result)
    }
}