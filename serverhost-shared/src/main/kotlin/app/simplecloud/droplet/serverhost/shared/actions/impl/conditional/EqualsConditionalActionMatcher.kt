package app.simplecloud.droplet.serverhost.shared.actions.impl.conditional

object EqualsConditionalActionMatcher : ConditionalActionMatcher {
    override fun match(first: String, second: String): Boolean {
        return first == second
    }
}