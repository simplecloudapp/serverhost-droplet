package app.simplecloud.droplet.serverhost.shared.actions.impl.download.util

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object DownloadUtil {

    fun ensureDestinationPathExists(path: Path, initDirIfMissing: Boolean) {
        when {
            path.exists() && !path.isDirectory() -> {
                throw IllegalArgumentException("Destination path is not a directory: $path")
            }

            !path.exists() && initDirIfMissing -> {
                Files.createDirectories(path)
            }
        }
    }

    fun resolveFilePath(url: String, destinationPath: Path): Path {
        //provided Path is a File
        if (Files.isRegularFile(destinationPath)) {
            return destinationPath
        }

        //provided Path is a Directory
        val fileName = url.substringAfterLast("/")
        return destinationPath.resolve(fileName)
    }

    suspend fun downloadFile(httpClient: HttpClient, url: String, destinationFile: Path) {
        try {
            val response: HttpResponse = httpClient.get(url)
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Failed to download file: ${response.status}")
            }

            val channel = response.bodyAsChannel()
            destinationFile.toFile().outputStream().buffered().use { outputStream -> channel.copyTo(outputStream) }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to download file: $url", e)
        }
    }
}