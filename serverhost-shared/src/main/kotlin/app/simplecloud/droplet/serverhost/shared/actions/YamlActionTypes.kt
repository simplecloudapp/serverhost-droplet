package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.actions.impl.*
import app.simplecloud.droplet.serverhost.shared.actions.impl.download.DownloadAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.download.modrinth.ModrinthDownloadAction

//All valid action types are listed here.
enum class YamlActionTypes(val action: YamlAction<*>) {
    COPY(CopyAction),
    DELETE(DeleteAction),
    PLACEHOLDER(PlaceholderAction),
    INFER(InferFromServerAction),
    CONFIGURATE(ConfigurateAction),
    DOWNLOAD(DownloadAction),
    MODRINTHDOWNLOAD(ModrinthDownloadAction);
}