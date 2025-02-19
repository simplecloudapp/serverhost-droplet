package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.actions.impl.codec.compress.CompressAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.codec.decompress.DecompressAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.conditional.ConditionalAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.configurate.ConfigurateAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.copy.CopyAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.create.dir.CreateDirAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.create.image.CreateImageAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.delete.DeleteAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.download.DownloadAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.download.GithubDownloadAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.download.modrinth.ModrinthDownloadAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.env.EnvAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.execute.ExecuteAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.infer.InferFromServerAction
import app.simplecloud.droplet.serverhost.shared.actions.impl.placeholder.PlaceholderAction

//All valid action types are listed here.
enum class YamlActionTypes(val action: YamlAction<*>) {
    COPY(CopyAction),
    DELETE(DeleteAction),
    PLACEHOLDER(PlaceholderAction),
    INFER(InferFromServerAction),
    CONFIGURATE(ConfigurateAction),
    DOWNLOAD(DownloadAction),
    GITHUB_DOWNLOAD(GithubDownloadAction),
    MODRINTH_DOWNLOAD(ModrinthDownloadAction),
    COMPRESS(CompressAction),
    DECOMPRESS(DecompressAction),
    CONDITIONAL(ConditionalAction),
    CREATE_DIR(CreateDirAction),
    ENV(EnvAction),
    CREATE_IMAGE(CreateImageAction),
    EXECUTE(ExecuteAction);
}