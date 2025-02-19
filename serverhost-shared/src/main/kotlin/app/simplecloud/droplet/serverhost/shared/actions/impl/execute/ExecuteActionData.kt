package app.simplecloud.droplet.serverhost.shared.actions.impl.execute

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ExecuteActionData(
    val command: String,
    val workingDirectory: String?,
    val environment: Map<String, String>?
)