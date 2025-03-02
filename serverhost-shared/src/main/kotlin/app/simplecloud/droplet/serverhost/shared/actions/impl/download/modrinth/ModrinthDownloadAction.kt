package app.simplecloud.droplet.serverhost.shared.actions.impl.download.modrinth

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.impl.download.util.DownloadUtil
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.logging.log4j.LogManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

object ModrinthDownloadAction : YamlAction<ModrinthDownloadActionData> {

    private const val BASE_API_URL = "https://api.modrinth.com/v2"
    private val logger = LogManager.getLogger(ModrinthDownloadAction::class.java)

    private fun httpClient(): HttpClient {
        return HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 15000
            }
            install(HttpRequestRetry) {
                maxRetries = 3
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
            }
        }
    }

    private data class ProjectInfo(
        var name: String = "",
        var versionNumber: String = "",
        val gameVersions: List<String> = mutableListOf(),
        val loaders: List<String> = mutableListOf()
    )

    override fun exec(ctx: YamlActionContext, data: ModrinthDownloadActionData): Unit = runBlocking {
        try {
            val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
                ?: throw IllegalStateException("Placeholder context is required but was not found")

            val modId = placeholders.parse(data.modId)
            val loader = placeholders.parse(data.loader)
            val gameVersion = placeholders.parse(data.gameVersion)

            withContext(Dispatchers.IO) {
                httpClient().use { client ->
                    val projectInfo = retrievePluginInformation(client, modId)
                    validateInputs(client, modId, loader, gameVersion, projectInfo)

                    val downloadUrl = retrievePluginDownloadUrl(client, modId, loader, gameVersion, projectInfo)
                    require(downloadUrl.isNotBlank()) { "Download URL is blank, please check your provided parameters" }

                    val fileName = "${projectInfo.name}-$gameVersion-${projectInfo.versionNumber}.jar"
                    val destinationPath = Paths.get(placeholders.parse(data.path))

                    handleFileDownload(
                        client = client,
                        destinationPath = destinationPath,
                        fileName = fileName,
                        downloadUrl = downloadUrl,
                        update = data.update,
                        initDirIfMissing = data.initDirIfMissing,
                        asFile = data.asFile,
                        cachePath = Paths.get(placeholders.parse("/%templates%/cache/modrinth/")).resolve(loader)
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e)
        }
    }

    override fun getDataType(): Class<ModrinthDownloadActionData> {
        return ModrinthDownloadActionData::class.java
    }

    private suspend fun retrievePluginInformation(client: HttpClient, id: String): ProjectInfo {
        val url = "$BASE_API_URL/project/$id"
        val response = client.get(url)
        require(response.status == HttpStatusCode.OK) { "Failed to retrieve plugin info: ${response.status}" }

        val jsonElement = Json.parseToJsonElement(response.body<String>())
        return jsonElement.jsonObject.let { json ->
            ProjectInfo(
                name = json["slug"]?.jsonPrimitive?.content ?: throw IllegalStateException("Plugin slug not found"),
                gameVersions = json["game_versions"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList(),
                loaders = json["loaders"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList()
            )
        }
    }

    private suspend fun validateInputs(client: HttpClient, modId: String, loader: String, gameVersion: String, projectInfo: ProjectInfo) {
        require(isModIdAvailable(client, modId)) { "Error verifying mod ID $modId" }
        require(isLoaderValid(loader, projectInfo)) { "Invalid loader $loader. Available loaders: ${projectInfo.loaders.joinToString()}" }
        require(isGameVersionValid(gameVersion, projectInfo)) { "Invalid game version $gameVersion. Available versions: ${projectInfo.gameVersions.joinToString()}" }
    }

    private suspend fun isModIdAvailable(client: HttpClient, modId: String): Boolean {
        val response = client.get("$BASE_API_URL/project/$modId")
        return response.status == HttpStatusCode.OK
    }

    private fun isLoaderValid(loader: String, projectInfo: ProjectInfo): Boolean =
        projectInfo.loaders.contains(loader)

    private fun isGameVersionValid(gameVersion: String, projectInfo: ProjectInfo): Boolean =
        projectInfo.gameVersions.contains(gameVersion)

    private suspend fun retrievePluginDownloadUrl(client: HttpClient, modId: String, loader: String, gameVersion: String, projectInfo: ProjectInfo): String {
        val url = "$BASE_API_URL/project/$modId/version?loaders=[%22$loader%22]&game_versions=[%22$gameVersion%22]"
        val response = client.get(url)
        require(response.status == HttpStatusCode.OK) { "Failed to retrieve version info: ${response.status}" }

        val jsonElement = Json.parseToJsonElement(response.body<String>())
        val firstVersion = jsonElement.jsonArray.firstOrNull()
            ?: throw IllegalStateException("No version found for the specified criteria")

        projectInfo.versionNumber = firstVersion.jsonObject["version_number"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Version number not found")

        return firstVersion.jsonObject["files"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
            ?: throw IllegalStateException("No download URL found")
    }

    private suspend fun handleFileDownload(
        client: HttpClient,
        destinationPath: Path,
        fileName: String,
        downloadUrl: String,
        update: Boolean,
        initDirIfMissing: Boolean,
        asFile: Boolean,
        cachePath: Path
    ) = withContext(Dispatchers.IO) {
        val destinationFile: Path = if (asFile) {
            destinationPath
        } else {
            destinationPath.resolve(fileName)
        }

        val cacheFile = cachePath.resolve(fileName)

        when {
            !update && destinationFile.exists() ->
                throw IllegalStateException("File exists and update is disabled: $destinationFile")

            !update && cacheFile.exists() -> {
                logger.info("Using cached file: $fileName")
                Files.createDirectories(destinationPath)
                Files.copy(cacheFile, destinationFile)
            }

            else -> {
                if (initDirIfMissing) {
                    createDirectoriesIfNeeded(destinationPath, cachePath)
                }

                logger.info("Downloading file: $fileName")
                DownloadUtil.downloadFile(client, downloadUrl, destinationFile)

                Files.copy(destinationFile, cacheFile)
            }
        }
    }


    private suspend fun createDirectoriesIfNeeded(vararg paths: Path) = withContext(Dispatchers.IO) {
        paths.forEach { path ->
            if (!Files.exists(path)) {
                Files.createDirectories(path)
            }
        }
    }
}
