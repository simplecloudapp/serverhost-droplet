package app.simplecloud.droplet.serverhost.shared.actions.impl.download.registry

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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

object SimpleCloudRegistryDownloadAction : YamlAction<SimpleCloudRegistryDownloadActionData> {

    private const val BASE_API_URL = "https://registry.simplecloud.app/v1"
    private val logger = LogManager.getLogger(SimpleCloudRegistryDownloadAction::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class UpdateCheckResponse(
        val updateAvailable: Boolean,
        val latestVersion: String,
        val manualUpdate: Boolean,
        val downloadUrl: String
    )

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

    override fun exec(ctx: YamlActionContext, data: SimpleCloudRegistryDownloadActionData): Unit = runBlocking {
        try {
            val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
                ?: throw IllegalStateException("Placeholder context is required but was not found")

            val appSlug = placeholders.parse(data.appSlug)
            val platform = placeholders.parse(data.platform)
            val arch = placeholders.parse(data.arch)
            val platformVersion = data.platformVersion?.let { placeholders.parse(it) }

            withContext(Dispatchers.IO) {
                httpClient().use { client ->
                    // Get the download URL
                    val downloadUrl = getDownloadUrl(client, appSlug, platform, arch, platformVersion)
                    logger.info("Download URL: $downloadUrl")

                    val fileName = "${appSlug}-${platform}-${arch}.jar"
                    val destinationPath = Paths.get(placeholders.parse(data.path))

                    handleFileDownload(
                        client = client,
                        destinationPath = destinationPath,
                        fileName = fileName,
                        downloadUrl = downloadUrl,
                        update = data.update,
                        initDirIfMissing = data.initDirIfMissing,
                        asFile = data.asFile,
                        cachePath = Paths.get(placeholders.parse("/%templates%/cache/registry/")).resolve(platform).resolve(arch)
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to download from registry", e)
            throw e
        }
    }

    private suspend fun getDownloadUrl(
        client: HttpClient,
        appSlug: String,
        platform: String,
        arch: String,
        platformVersion: String?
    ): String {
        val url = buildString {
            append("$BASE_API_URL/applications/$appSlug/download/latest")
            append("?platform=$platform&arch=$arch")
            if (platformVersion != null) {
                append("&platform_version=$platformVersion")
            }
        }

        val response = client.get(url)
        require(response.status == HttpStatusCode.OK) {
            "Failed to get download URL for ${url}: ${response.status}"
        }

        return url
    }

    override fun getDataType(): Class<SimpleCloudRegistryDownloadActionData> {
        return SimpleCloudRegistryDownloadActionData::class.java
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
                logger.info("File exists and update is disabled: $destinationFile")

            !update && cacheFile.exists() -> {
                logger.info("Using cached file: $fileName")
                Files.createDirectories(destinationPath)
                Files.copy(cacheFile, destinationFile)
            }

            else -> {
                if (initDirIfMissing) {
                    createDirectoriesIfNeeded(destinationPath, cachePath)
                }

                logger.info("Downloading file: $fileName from $downloadUrl")
                val response = client.get(downloadUrl) {
                    headers {
                        append(HttpHeaders.Accept, "application/octet-stream")
                    }
                }
                require(response.status == HttpStatusCode.OK) {
                    "Failed to download file: ${response.status}"
                }

                Files.createDirectories(destinationFile.parent)
                Files.write(destinationFile, response.body<ByteArray>())

                Files.createDirectories(cacheFile.parent)
                Files.copy(destinationFile, cacheFile, StandardCopyOption.REPLACE_EXISTING)
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
