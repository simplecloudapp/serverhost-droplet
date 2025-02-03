package app.simplecloud.droplet.serverhost.shared.actions.impl.conditional

interface ConditionalActionMatcher {
    fun match(first: String, second: String): Boolean
}