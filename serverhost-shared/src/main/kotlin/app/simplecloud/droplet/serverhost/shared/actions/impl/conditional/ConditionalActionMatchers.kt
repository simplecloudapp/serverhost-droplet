package app.simplecloud.droplet.serverhost.shared.actions.impl.conditional

enum class ConditionalActionMatchers(val matcher: ConditionalActionMatcher) {
    EQUALS(EqualsConditionalActionMatcher)
}