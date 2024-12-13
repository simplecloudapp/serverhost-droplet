package app.simplecloud.droplet.serverhost.shared.actions.impl.codec.decompress

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.impl.codec.ArchiveType
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

object DecompressAction : YamlAction<DecompressActionData> {

    override fun exec(ctx: YamlActionContext, data: DecompressActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw IllegalStateException("Placeholder context is required but was not found.")

        val parsedPath = placeholders.parse(data.path).takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Archive path cannot be empty.")

        val archiveFilePath = Paths.get(parsedPath)
        val archiveFile = archiveFilePath.toFile()
        require(archiveFile.exists()) { "Archive file does not exist: ${archiveFile.absolutePath}." }
        require(archiveFile.isFile) { "${archiveFile.absolutePath} is not a file." }

        val destinationPath = placeholders.parse(data.dest).let {
            if (it.isBlank()) archiveFilePath.parent.toAbsolutePath()
            else Paths.get(it)
        }

        require(!(data.replace && destinationPath.toFile().exists())) {
            "File already exists and replace is set to false: $destinationPath."
        }

        if (!Files.exists(destinationPath)) {
            try {
                Files.createDirectories(destinationPath)
            } catch (e: IOException) {
                throw IllegalStateException("Failed to create directory: $destinationPath.", e)
            }
        } else {
            require(Files.isDirectory(destinationPath)) { "$destinationPath is not a directory." }
        }

        try {
            val archiveType = resolveArchiveType(archiveFilePath)

            when (archiveType) {
                ArchiveType.TAR_GZ, ArchiveType.TGZ, ArchiveType.TAR_BZ2, ArchiveType.TBZ2 ->
                    extractCompressed(archiveFile, destinationPath, data.replace)

                ArchiveType.ZIP, ArchiveType.TAR ->
                    extractArchive(archiveFile, destinationPath, data.replace)

                ArchiveType.SEVEN_Z ->
                    extract7z(archiveFile, destinationPath, data.replace)
            }

        } catch (e: ArchiveException) {
            println("Error processing archive: ${e.message}")
        } catch (e: CompressorException) {
            println("Error with compression: ${e.message}")
        } catch (e: IOException) {
            println("Error reading/writing file: ${e.message}")
        } catch (e: UnsupportedOperationException) {
            println(e.message)
        }
    }

    override fun getDataType(): Class<DecompressActionData> {
        return DecompressActionData::class.java
    }

    private fun extractCompressed(archiveFile: File, destinationPath: Path, replace: Boolean) {
        try {
            BufferedInputStream(FileInputStream(archiveFile)).use { bufferedInputStream ->
                CompressorStreamFactory().createCompressorInputStream(bufferedInputStream)
                    .use { compressorInputStream ->
                        (ArchiveStreamFactory().createArchiveInputStream(
                            "tar",
                            compressorInputStream
                        ) as ArchiveInputStream<ArchiveEntry>).use { archiveInputStream ->
                            extractEntries(archiveInputStream, destinationPath, replace)
                        }
                    }
            }
        } catch (e: Exception) {
            println("Error extracting archive: ${e.message}")
        }
    }

    private fun extractArchive(archiveFile: File, destinationPath: Path, replace: Boolean) {
        BufferedInputStream(FileInputStream(archiveFile)).use { bufferedInputStream ->
            (ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream) as ArchiveInputStream<ArchiveEntry>).use { archiveInputStream ->
                extractEntries(archiveInputStream, destinationPath, replace)
            }
        }
    }

    private fun extract7z(archiveFile: File, destinationPath: Path, replace: Boolean) {
        SevenZFile.builder()
            .setFile(archiveFile)
            .setCharset(Charsets.UTF_8)
            .get().use { sevenZFile ->
                var entry: SevenZArchiveEntry?
                while (sevenZFile.nextEntry.also { entry = it } != null) {
                    val entryName = entry!!.name
                    val sanitizedEntryName = entryName.replace("..", "")
                    val outputFilePath = destinationPath.resolve(sanitizedEntryName)

                    if (entry!!.isDirectory) {
                        Files.createDirectories(outputFilePath)
                    } else {
                        Files.createDirectories(outputFilePath.parent)
                        if (replace) {
                            Files.deleteIfExists(outputFilePath)
                        }
                        FileOutputStream(outputFilePath.toFile()).use { outputStream ->
                            val buffer = ByteArray(4096)
                            var bytesRead: Int
                            while (sevenZFile.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                }
            }
    }

    private fun extractEntries(archiveInputStream: ArchiveInputStream<*>, destinationPath: Path, replace: Boolean) {
        val buffer = ByteArray(4096)
        var count: Int

        var entry: ArchiveEntry? = archiveInputStream.nextEntry
        while (entry != null) {
            val entryName = entry.name
            val sanitizedEntryName = entryName.replace("..", "")
            val outputFilePath = destinationPath.resolve(sanitizedEntryName)

            if (entry.isDirectory) {
                Files.createDirectories(outputFilePath)
            } else {
                Files.createDirectories(outputFilePath.parent)
                if (replace) {
                    Files.deleteIfExists(outputFilePath)
                }
                FileOutputStream(outputFilePath.toFile()).use { outputStream ->
                    while (archiveInputStream.read(buffer).also { count = it } != -1) {
                        outputStream.write(buffer, 0, count)
                    }
                }
            }
            entry = archiveInputStream.nextEntry
        }
    }

    private fun resolveArchiveType(archiveFile: Path): ArchiveType {
        val fileName = archiveFile.fileName.toString().lowercase(Locale.getDefault())
        return ArchiveType.entries.firstOrNull { fileName.endsWith(it.extension) }
            ?: throw IllegalArgumentException("Unsupported archive format: $fileName")
    }
}
