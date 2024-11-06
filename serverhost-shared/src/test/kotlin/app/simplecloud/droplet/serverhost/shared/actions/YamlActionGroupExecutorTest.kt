package app.simplecloud.droplet.serverhost.shared.actions

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.nio.file.Paths

class YamlActionGroupExecutorTest {

    private val directory = Paths.get("actions")

    @Test
    fun execute() {
        val executor = YamlActionGroupExecutor(YamlActionLoader.load(directory))
        executor.execute("cache/cache-paper")
    }
}