package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.actions.impl.CopyAction

//All valid action types are listed here.
enum class YamlActionTypes(val action: YamlAction<*>) {
    COPY(CopyAction);
}