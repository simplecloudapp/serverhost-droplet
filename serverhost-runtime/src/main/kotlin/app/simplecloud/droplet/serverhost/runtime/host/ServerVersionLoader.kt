package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.server.Server
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ServerVersionLoader {

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

    fun download(server: Server) {
        val destination = getServerJar(server)
        if (!destination.exists()) {
            download(server.properties["server-url"]!!, destination)
        }
    }

    fun getServerJar(server: Server): File {
        return File(
            "cache/servers/${
                FilenameUtils.getBaseName(URL(server.properties["server-url"]).path).uppercase()
            }.jar"
        )
    }
}