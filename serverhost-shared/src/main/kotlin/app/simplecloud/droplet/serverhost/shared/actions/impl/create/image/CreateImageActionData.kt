package app.simplecloud.droplet.serverhost.shared.actions.impl.create.image

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CreateImageActionData(
    val imageName: String,
    val tags: List<String> = listOf(),
    val context: String,
    val cacheFrom: List<String> = listOf(),
    val cacheTo: List<String> = listOf(),
    val buildType: ImageBuildType = ImageBuildType.DAEMON,
    val tarDest: String? = null,
    val configurator: String,
    val base: String = "eclipse-temurin:21-jre-alpine",
)
