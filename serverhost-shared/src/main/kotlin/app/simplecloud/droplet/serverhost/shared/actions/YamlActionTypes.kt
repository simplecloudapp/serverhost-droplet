package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.actions.impl.*
import app.simplecloud.droplet.serverhost.shared.actions.impl.codec.compress.CompressAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.codec.decompress.DecompressAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.conditional.ConditionalAction
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
    MODRINTH_DOWNLOAD(ModrinthDownloadAction),
    COMPRESS(CompressAction),
    DECOMPRESS(DecompressAction),
    CONDITIONAL(ConditionalAction),
    CREATE_DIR(CreateDirAction);
}