package app.simplecloud.droplet.serverhost.runtime.config.environment

enum class BuildPolicy(val trigger: Boolean = false, val firstBuild: Boolean = false) {
    NEVER,
    ALWAYS(firstBuild = true, trigger = true),
    ONCE(firstBuild = true),
    TRIGGER(trigger = true),
    ONCE_AND_TRIGGER(trigger = true, firstBuild = true),
}