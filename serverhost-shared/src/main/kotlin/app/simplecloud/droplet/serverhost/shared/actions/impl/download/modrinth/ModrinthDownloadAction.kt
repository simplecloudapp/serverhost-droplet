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
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

object ModrinthDownloadAction : YamlAction<ModrinthDownloadActionData> {

    private const val BASE_API_URL = "https://api.modrinth.com/v2"
    private val logger = LogManager.getLogger(ModrinthDownloadAction::class.java)

    private val httpClient by lazy {
        HttpClient(CIO) {
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
        val gameVersions: MutableList<String> = mutableListOf(),
        val loaders: MutableList<String> = mutableListOf()
    )

    private var projectInfo = ProjectInfo()

    override fun exec(ctx: YamlActionContext, data: ModrinthDownloadActionData): Unit = runBlocking {
        try {
            val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
                ?: throw IllegalStateException("Placeholder context is required but was not found")

            val modId = placeholders.parse(data.modId)
            val loader = placeholders.parse(data.loader)
            val gameVersion = placeholders.parse(data.gameVersion)

            withContext(Dispatchers.IO) {
                httpClient.use {
                    retrievePluginInformation(modId)
                    validateInputs(modId, loader, gameVersion)

                    val downloadUrl = retrievePluginDownloadUrl(modId, loader, gameVersion)
                    require(downloadUrl.isNotBlank()) { "Download URL is blank, please check your provided parameters" }

                    val fileName = "${projectInfo.name}-$gameVersion-${projectInfo.versionNumber}.jar"
                    val destinationPath = Paths.get(placeholders.parse(data.path))

                    handleFileDownload(
                        destinationPath = destinationPath,
                        fileName = fileName,
                        downloadUrl = downloadUrl,
                        update = data.update,
                        initDirIfMissing = data.initDirIfMissing,
                        cachePath = Paths.get(placeholders.parse("/%templates%/cache/modrinth/")).resolve(loader)
                    )
                }
            }
        } finally {
            cleanup()
        }
    }

    override fun getDataType(): Class<ModrinthDownloadActionData> {
        return ModrinthDownloadActionData::class.java
    }

    private suspend fun retrievePluginInformation(id: String) {
        val url = "$BASE_API_URL/project/$id"
        val response = httpClient.get(url)
        require(response.status == HttpStatusCode.OK) { "Failed to retrieve plugin info: ${response.status}" }

        val jsonElement = Json.parseToJsonElement(response.body<String>())
        jsonElement.jsonObject.let { json ->
            projectInfo.apply {
                name = json["slug"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("Plugin slug not found")
                gameVersions.addAll(json["game_versions"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList())
                loaders.addAll(json["loaders"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: emptyList())
            }
        }
    }

    private suspend fun validateInputs(modId: String, loader: String, gameVersion: String) {
        require(isModIdAvailable(modId)) { "Error verifying mod ID $modId" }
        require(isLoaderValid(loader)) { "Invalid loader $loader. Available loaders: ${projectInfo.loaders.joinToString()}" }
        require(isGameVersionValid(gameVersion)) { "Invalid game version $gameVersion. Available versions: ${projectInfo.gameVersions.joinToString()}" }
    }

    private suspend fun isModIdAvailable(modId: String): Boolean {
        val response = httpClient.get("$BASE_API_URL/project/$modId")
        return response.status == HttpStatusCode.OK
    }

    private fun isLoaderValid(loader: String): Boolean =
        projectInfo.loaders.contains(loader)

    private fun isGameVersionValid(gameVersion: String): Boolean =
        projectInfo.gameVersions.contains(gameVersion)

    private suspend fun retrievePluginDownloadUrl(modId: String, loader: String, gameVersion: String): String {
        val url = "$BASE_API_URL/project/$modId/version?loaders=[%22$loader%22]&game_versions=[%22$gameVersion%22]"
        val response = httpClient.get(url)
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
        destinationPath: Path,
        fileName: String,
        downloadUrl: String,
        update: Boolean,
        initDirIfMissing: Boolean,
        cachePath: Path
    ) = withContext(Dispatchers.IO) {
        val destinationFile = destinationPath.resolve(fileName)
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
                DownloadUtil.downloadFile(httpClient, downloadUrl, destinationFile)
                println("CACHE: ->>>>>>>>>>>>>>>>>>>>>> ${cacheFile.absolutePathString()}")
                Files.copy(destinationFile, cacheFile)
            }
        }
    }

    private fun cleanup() {
        projectInfo = ProjectInfo()
    }

    private suspend fun createDirectoriesIfNeeded(vararg paths: Path) = withContext(Dispatchers.IO) {
        paths.forEach { path ->
            if (!Files.exists(path)) {
                Files.createDirectories(path)
            }
        }
    }
}