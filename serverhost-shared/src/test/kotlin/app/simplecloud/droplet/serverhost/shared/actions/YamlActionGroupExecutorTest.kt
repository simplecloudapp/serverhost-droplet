package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.ResourcePath
import app.simplecloud.droplet.serverhost.shared.YamlActionPlaceholderContext
import org.junit.jupiter.api.Test

import java.nio.file.Path

class YamlActionGroupExecutorTest {

    private val directory = ResourcePath.get("actions")

    @Test
    fun execute() {
        val ctx = YamlActionContext()
        val placeholderCtx = YamlActionPlaceholderContext()
        placeholderCtx.setGroup("test")
        placeholderCtx.setLibs(Path.of("libs"))
        placeholderCtx.setTemplate(Path.of("templates"))
        placeholderCtx.setServerPath(Path.of("servers", "test-1"))
        placeholderCtx.save(ctx)
        val executor = YamlActionGroupExecutor(ctx, YamlActionLoader.load(directory))
        executor.execute("cache/cache-paper")
    }
}