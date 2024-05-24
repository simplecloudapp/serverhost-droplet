package app.simplecloud.droplet.serverhost.runtime.host

import org.apache.commons.io.FilenameUtils
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object ServerVersionLoader {

    private const val CACHE_PATH_STRING = "cache/servers/"

    fun getAndDownloadServerJar(serverUriString: String): File {
        val serverUri = URI.create(serverUriString)
        val path = serverUri.path
        if (serverUri.scheme == null || serverUri.scheme == "file") {
            if (path.startsWith("/")) {
                return File(path)
            }

            return File(CACHE_PATH_STRING, path)
        }

        val file = File(
            CACHE_PATH_STRING,
            "${FilenameUtils.getBaseName(path).uppercase()}.jar"
        )

        if (!file.exists()) {
            download(serverUriString, file)
        }

        return file
    }


    private fun download(url: String, file: File) {
        file.parentFile?.mkdirs()
        val urlConnection = URL(url).openConnection()
        urlConnection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:58.0) Gecko/20100101 Firefox/58.0"
        )
        urlConnection.connect()
        Files.copy(urlConnection.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

}