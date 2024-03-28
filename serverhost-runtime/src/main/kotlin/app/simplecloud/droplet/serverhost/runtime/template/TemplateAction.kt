package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.droplet.serverhost.runtime.template.impl.DefaultTemplateExecutor
import app.simplecloud.droplet.serverhost.runtime.template.impl.RandomTemplateExecutor
import app.simplecloud.droplet.serverhost.runtime.template.impl.ShutdownTemplateExecutor
import org.spongepowered.configurate.objectmapping.ConfigSerializable


@ConfigSerializable
data class TemplateAction(
    val copyFrom: String,
    val copyTo: String,
)

enum class TemplateActionType {
    DEFAULT {
        private val executor = DefaultTemplateExecutor()

        override fun executor(): TemplateActionExecutor {
            return executor
        }
    },
    RANDOM {
        private val executor = RandomTemplateExecutor()

        override fun executor(): TemplateActionExecutor {
            return executor
        }
    },
    SHUTDOWN {
        private val executor = ShutdownTemplateExecutor()

        override fun executor(): TemplateActionExecutor {
            return executor
        }
    };

    abstract fun executor(): TemplateActionExecutor
}