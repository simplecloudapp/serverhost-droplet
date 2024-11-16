package app.simplecloud.droplet.serverhost.shared.actions.impl

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext

object CopyAction: YamlAction<CopyActionData> {

    override fun exec(ctx: YamlActionContext, data: CopyActionData) {
        //TODO: Actually implement actions
        println("Copying from ${data.from} to ${data.to}")
    }

    override fun getDataType(): Class<CopyActionData> {
        return CopyActionData::class.java
    }
}