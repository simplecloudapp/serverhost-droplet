package app.simplecloud.droplet.serverhost.shared.actions.impl.download

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.apache.logging.log4j.LogManager
import org.kohsuke.github.GHAsset
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * @author Niklas Nieberler
 */

object GithubDownloadAction : YamlAction<GithubDownloadActionData> {

    private val githubUrlInterceptor = Interceptor { chain ->
        val original = chain.request()
        val originalUrl = original.url.toString()

        val newUrl = originalUrl.replace(
            "api.github.com",
            "gha.simplecloud.app"
        )

        val newRequest = original.newBuilder()
            .url(newUrl)
            .build()

        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(githubUrlInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val logger = LogManager.getLogger(GithubDownloadAction::class.java)

    private val gitHub = connectToGitHub()

    override fun exec(ctx: YamlActionContext, data: GithubDownloadActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("Placeholder context is required but was not found")

        val outputFilePath = placeholders.parse(data.path)

        logger.info("Try to download asset ${data.assetName} from repository ${data.url}")

        val asset = getReleaseAsset(data.url, data.assetName, data.releaseTag)
        val outputFile = File(outputFilePath, asset.name)
        downloadAsset(asset.browserDownloadUrl, outputFile)
    }

    override fun getDataType() = GithubDownloadActionData::class.java

    private fun getReleaseAsset(url: String, assetName: String, releaseTag: String?): GHAsset {
        val releaseTag = getReleaseTag(url, releaseTag)
        this.logger.info("Release selected for $url: ${releaseTag.name} (${releaseTag.tagName})")
        return releaseTag.listAssets().firstOrNull { it.name == assetName }
            ?: throw IllegalStateException("failed to find $assetName in release ${releaseTag.tagName}")
    }

    private fun getReleaseTag(url: String, releaseTag: String?): GHRelease {
        val repositoryPath = extractRepoPathFromUrl(url)
        val repository = this.gitHub.getRepository(repositoryPath)
        if (releaseTag == null) {
            return repository.latestRelease
                ?: throw IllegalStateException("failed to find release for repository $repositoryPath")
        }
        return repository.getReleaseByTagName(releaseTag)
            ?: throw IllegalStateException("failed to find tag $releaseTag for repository $repositoryPath")
    }

    private fun extractRepoPathFromUrl(url: String): String {
        val pathSegments = URI(url).path.trim('/').split('/')
        if (pathSegments.size < 2)
            throw IllegalArgumentException("failed to find github repository $url")
        return "${pathSegments[0]}/${pathSegments[1]}"
    }

    private fun connectToGitHub(): GitHub {
        logger.info("Connecting to GitHub...")
        val builder = GitHubBuilder()
            .withConnector(OkHttpGitHubConnector(okHttpClient))

        return when {
            System.getenv("SC_GITHUB_TOKEN") != null -> {
                logger.info("Using GitHub token from environment variable SC_GITHUB_TOKEN")
                builder.withOAuthToken(System.getenv("SC_GITHUB_TOKEN")).build()
            }

            else -> {
                logger.info("Using anonymous GitHub connection")
                builder.build()
            }
        }
    }

    private fun downloadAsset(url: String, outputFile: File) {
        val connection = URI(url)
            .toURL()
            .openConnection()

        connection.setRequestProperty("Accept", "application/octet-stream")
        connection.setRequestProperty(
            "Authorization",
            "Bearer ${System.getenv("SC_GITHUB_TOKEN")}"
        )

        outputFile.delete()
        outputFile.parentFile?.mkdirs()
        outputFile.createNewFile()

        connection.inputStream.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

}