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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
            require(isModIdAvailable(modId)) { "Error verifying mod ID $modId" }
            require(isLoaderAvailable(loader)) { "Error verifying loader $loader" }
            require(isGameVersionAvailable(gameVersion)) { "Error verifying game version $gameVersion" }

            val downloadUrl = retrievePluginDownloadUrl(modId, loader, gameVersion)
            require(downloadUrl.isNotBlank()) { "Download URL is blank, please check your provided parameter" }

            val destinationPath = Paths.get(placeholders.parse(data.destinationPath))
            DownloadUtil.ensureDestinationPathExists(destinationPath, data.initMissingDirectories)

            val destinationFile = DownloadUtil.resolveFilePath(downloadUrl, destinationPath)
            if (destinationFile.exists() && !data.overwrite) {
                throw IllegalStateException("File already exists and overwrite is disabled: $destinationFile")
            }

            DownloadUtil.downloadFile(httpClient, downloadUrl, destinationFile)
        }
    }

    override fun getDataType(): Class<ModrinthDownloadActionData> {
        return ModrinthDownloadActionData::class.java
    }

    private suspend fun isModIdAvailable(modId: String): Boolean {
        try {
            val response: HttpResponse = httpClient.get("https://api.modrinth.com/v2/project/${modId}")

            return response.status.isSuccess()
        } catch (e: Exception) {
            throw IllegalStateException("Error occurred while fetching modId info", e)
        }
    }

    private suspend fun isLoaderAvailable(loader: String): Boolean {
        try {
            val response: HttpResponse = httpClient.get("https://api.modrinth.com/v2/tag/loader")

            if (response.status.isSuccess()) {
                val jsonArray = Json.parseToJsonElement(response.body()).jsonArray
                val availableLoader = jsonArray.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
                return availableLoader.contains(loader)
            }

            return false
        } catch (e: Exception) {
            throw IllegalStateException("Error occurred while fetching available loader", e)
        }
    }

    private suspend fun isGameVersionAvailable(loader: String): Boolean {
        try {
            val response: HttpResponse = httpClient.get("https://api.modrinth.com/v2/tag/game_version")

            if (response.status.isSuccess()) {
                val jsonArray = Json.parseToJsonElement(response.body()).jsonArray

                // Filter JsonArray to only go through versions tagged as "RELEASE"
                val availableVersions = jsonArray.filter {
                    it.jsonObject["version_type"]?.jsonPrimitive?.content == "release"
                }.mapNotNull {
                    it.jsonObject["version"]?.jsonPrimitive?.content
                }

                return availableVersions.contains(loader)
            }

            return false
        } catch (e: Exception) {
            throw IllegalStateException("Error occurred while fetching available game versions", e)
        }
    }

    private suspend fun retrievePluginDownloadUrl(modId: String, loader: String, gameVersion: String): String {
        try {

            val response: HttpResponse = httpClient.get(
                "https://api.modrinth.com/v2/project/${modId}/version?loaders=[${loader}]&game_versions=[${gameVersion}]"
            )

            if (response.status.isSuccess()) {
                val jsonArray = Json.parseToJsonElement(response.body()).jsonArray
                require(jsonArray.isNotEmpty()) { "Error verifying plugin data" }

                val filesArray = jsonArray.first().jsonObject["files"]?.jsonArray
                require(!filesArray.isNullOrEmpty()) { "Error occurred when trying to fetch file data from the Plugin $modId" }

                val url = filesArray.first().jsonObject["url"]?.jsonPrimitive?.content
                require(!url.isNullOrBlank()) { "Error occurred when trying to fetch url from the Plugin $modId" }

                return url
            }

            //Something went wrong while fetching downloadUrl
            return "";
        } catch (e: Exception) {
            throw IllegalStateException("Error occurred while fetching available game versions", e)
        }
    }
}