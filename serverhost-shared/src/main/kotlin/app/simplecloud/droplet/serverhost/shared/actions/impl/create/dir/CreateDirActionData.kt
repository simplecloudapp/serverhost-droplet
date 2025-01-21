package app.simplecloud.droplet.serverhost.shared.actions.impl.create.dir

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CreateDirActionData(val dir: String)
