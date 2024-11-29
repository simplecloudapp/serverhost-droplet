package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.ResourcePath
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import java.nio.file.Path

class YamlActionGroupExecutorTest {

    private val directory = ResourcePath.get("assert/actions")

    @Test
    fun execute() {
        val ctx = YamlActionContext()
        val placeholderCtx = YamlActionPlaceholderContext()
        placeholderCtx.setGroup("test")
        placeholderCtx.setLibs(Path.of("libs"))
        placeholderCtx.setTemplate(ResourcePath.get("assert/templates"))
        placeholderCtx.setServerDir(Path.of("src", "test", "resources", "servers", "test-1"))
        placeholderCtx.save(ctx)
        val executor = YamlActionGroupExecutor(ctx, YamlActionLoader.load(directory))
        val errors = executor.execute("cache/cache-pull")
        errors.forEach { it.printStackTrace() }
        assert(errors.isEmpty())
        FileUtils.deleteDirectory(Path.of("src", "test", "resources", "servers").toFile())
    }
}