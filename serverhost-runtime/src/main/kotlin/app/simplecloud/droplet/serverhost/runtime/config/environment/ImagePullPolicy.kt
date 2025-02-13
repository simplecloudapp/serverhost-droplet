package app.simplecloud.droplet.serverhost.runtime.config.environment

enum class ImagePullPolicy {
    NEVER,
    ALWAYS,
    IF_NOT_PRESENT,
}