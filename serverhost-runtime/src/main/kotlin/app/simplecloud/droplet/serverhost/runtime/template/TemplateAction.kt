package app.simplecloud.droplet.serverhost.runtime.template

import app.simplecloud.droplet.serverhost.runtime.template.impl.DefaultTemplateExecutor
import app.simplecloud.droplet.serverhost.runtime.template.impl.ShutdownTemplateExecutor
import org.spongepowered.configurate.objectmapping.ConfigSerializable


@ConfigSerializable
data class TemplateAction(
    val copyFrom: String,
    val copyTo: String,
)

enum class TemplateActionType(
    val executor: TemplateActionExecutor,
) {

    DEFAULT(DefaultTemplateExecutor()),
    RANDOM(DefaultTemplateExecutor()),
    SHUTDOWN(ShutdownTemplateExecutor());

}