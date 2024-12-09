package app.simplecloud.droplet.serverhost.shared.actions.impl.download.modrinth

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.impl.download.util.DownloadUtil
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.nio.file.Paths
import kotlin.io.path.exists

object ModrinthDownloadAction : YamlAction<ModrinthDownloadActionData> {

    private val httpClient by lazy { HttpClient() }

    override fun exec(ctx: YamlActionContext, data: ModrinthDownloadActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("Placeholder context is required but was not found")

        val modId = placeholders.parse(data.modId)
        val loader = placeholders.parse(data.loader)
        val gameVersion = placeholders.parse(data.gameVersion)
        runBlocking {
            httpClient.use { httpClient ->
                validateInputs(modId, loader, gameVersion)

                val downloadUrl = retrievePluginDownloadUrl(modId, loader, gameVersion)
                require(downloadUrl.isNotBlank()) { "Download URL is blank, please check your provided parameter" }

                val destinationPath = Paths.get(placeholders.parse(data.path))
                DownloadUtil.ensureDestinationPathExists(destinationPath, data.initDirIfMissing)

                val destinationFile = DownloadUtil.resolveFilePath(downloadUrl, destinationPath)
                if (destinationFile.exists() && !data.replace) {
                    throw IllegalStateException("File already exists and overwrite is disabled: $destinationFile")
                }

                DownloadUtil.downloadFile(httpClient, downloadUrl, destinationFile)
            }
        }
    }

    override fun getDataType(): Class<ModrinthDownloadActionData> {
        return ModrinthDownloadActionData::class.java
    }

    private suspend fun validateInputs(modId: String, loader: String, gameVersion: String) {
        require(isModIdAvailable(modId)) { "Error verifying mod ID $modId" }
        require(isLoaderValid(loader)) { "Error verifying loader $loader" }
        require(isGameVersionValid(gameVersion)) { "Error verifying game version $gameVersion" }
    }

    private suspend fun isModIdAvailable(modId: String): Boolean {
        return fetchJson("https://api.modrinth.com/v2/project/$modId") != null
    }

    private suspend fun isLoaderValid(loader: String): Boolean {
        val response = fetchJson("https://api.modrinth.com/v2/tag/loader")
        val availableLoaders = response?.jsonArray?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
        return availableLoaders?.contains(loader) == true
    }

    private suspend fun isGameVersionValid(gameVersion: String): Boolean {
        val response = fetchJson("https://api.modrinth.com/v2/tag/game_version")
        val availableVersions = response?.jsonArray
            ?.filter { it.jsonObject["version_type"]?.jsonPrimitive?.content == "release" }
            ?.mapNotNull { it.jsonObject["version"]?.jsonPrimitive?.content }
        return availableVersions?.contains(gameVersion) == true
    }

    private suspend fun retrievePluginDownloadUrl(modId: String, loader: String, gameVersion: String): String {
        val url = "https://api.modrinth.com/v2/project/$modId/version?loaders=[$loader]&game_versions=[$gameVersion]"
        val response = fetchJson(url)
        val filesArray = response?.jsonArray?.firstOrNull()?.jsonObject?.get("files")?.jsonArray
        return filesArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
    }

    private suspend fun fetchJson(url: String): JsonElement? {
        return try {
            val response: HttpResponse = httpClient.get(url)
            if (response.status.isSuccess()) { // Check for a successful response
                Json.parseToJsonElement(response.body()) // Parse the response body as JSON
            } else {
                null
            }
        } catch (e: Exception) {
            throw IllegalStateException("Error occurred while fetching data from $url", e)
        }
    }
}