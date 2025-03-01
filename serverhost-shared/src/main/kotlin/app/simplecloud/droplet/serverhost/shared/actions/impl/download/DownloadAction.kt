package app.simplecloud.droplet.serverhost.shared.actions.impl.download

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.impl.download.util.DownloadUtil
import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.nio.file.Paths
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists

object DownloadAction : YamlAction<DownloadActionData> {

    private fun httpClient(): HttpClient {
        return HttpClient()
    }

    override fun exec(ctx: YamlActionContext, data: DownloadActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("Placeholder context is required but was not found")

        val url = placeholders.parse(data.url)
        require(isValidUrl(url)) { "Invalid URL: $url" }

        val destinationPath = Paths.get(placeholders.parse(data.path))
        DownloadUtil.ensureDestinationPathExists(destinationPath, data.initDirIfMissing)

        val file = DownloadUtil.resolveFilePath(url, destinationPath)
        if (file.exists() && !data.replace) {
            throw IllegalStateException("File already exists and overwrite is disabled: $file")
        }

        if (file.exists()) {
            file.deleteExisting()
        }

        runBlocking {
            httpClient().use {
                DownloadUtil.downloadFile(it, url, file)
            }

        }
    }

    override fun getDataType(): Class<DownloadActionData> {
        return DownloadActionData::class.java
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = URI(url);

            uri.scheme != null && uri.host != null;
        } catch (e: Exception) {
            false
        }
    }
}