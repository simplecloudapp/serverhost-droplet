package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.actions.impl.CopyAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.DeleteAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.PlaceholderAction

//All valid action types are listed here.
enum class YamlActionTypes(val action: YamlAction<*>) {
    COPY(CopyAction),
    DELETE(DeleteAction),
    PLACEHOLDER(PlaceholderAction),
}