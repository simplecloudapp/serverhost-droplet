package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.actions.impl.CopyAction

enum class YamlActionTypes(val action: YamlAction<*>) {
    COPY(CopyAction);
}