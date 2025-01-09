package app.simplecloud.droplet.serverhost.shared.actions.impl.create.image

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CreateImageActionData(
    val imageName: String,
    val tags: List<String> = listOf(),
    val context: String,
    val cacheFrom: List<String> = listOf(),
    val cacheTo: List<String> = listOf(),
    val exposedPort: Int = 25565,
    val buildType: ImageBuildType = ImageBuildType.DAEMON,
    val tarDest: String? = null,
    val base: String = "openjdk:21-slim",
)
