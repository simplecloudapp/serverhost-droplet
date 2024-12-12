package app.simplecloud.droplet.serverhost.shared.actions.impl.codec.compress

import app.simplecloud.droplet.serverhost.shared.actions.YamlAction
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionContext
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionPlaceholderContext
import app.simplecloud.droplet.serverhost.shared.actions.impl.codec.ArchiveType
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

object CompressAction : YamlAction<CompressActionData> {

    override fun exec(ctx: YamlActionContext, data: CompressActionData) {
        val placeholders = YamlActionPlaceholderContext.retrieve(ctx)
            ?: throw NullPointerException("Placeholder context is required but was not found")

        val sourcePath = placeholders.parse(data.directory)
        require(sourcePath.isNotBlank()) { "Source path cannot be empty." }

        val archiveType = try {
            getArchiveTypeFromExtension(placeholders.parse(data.format))
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid archive format: ${data.format}")
        }

        val sourceDirPath = Paths.get(sourcePath)
        require(Files.exists(sourceDirPath)) { "Source directory does not exist: ${sourceDirPath.toAbsolutePath()}." }
        require(Files.isDirectory(sourceDirPath)) { "${sourceDirPath.toAbsolutePath()} is not a directory." }

        val dataDest = Paths.get(placeholders.parse(data.dest))
        val destinationFileName = "${sourceDirPath.fileName}${archiveType.extension}"
        val destinationPath = dataDest.resolve(destinationFileName)
        val destinationFile = destinationPath.toFile()

        Files.createDirectories(destinationPath.parent)

        if (destinationFile.exists()) {
            if (data.replace) {
                destinationFile.delete()
            } else {
                throw IllegalArgumentException("Destination file already exists: ${destinationFile.absolutePath}")
            }
        }

        try {
            when (archiveType) {
                ArchiveType.TAR_GZ, ArchiveType.TGZ, ArchiveType.TAR_BZ2, ArchiveType.TBZ2,
                ArchiveType.ZIP, ArchiveType.TAR -> {
                    compressToArchive(sourceDirPath, destinationPath, archiveType)
                }

                ArchiveType.SEVEN_Z -> {
                    compressTo7z(sourceDirPath, destinationPath)
                }
            }
        } catch (e: Exception) {
            println("Error during compression: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getDataType(): Class<CompressActionData> {
        return CompressActionData::class.java
    }

    private fun compressToArchive(sourcePath: Path, archiveFilePath: Path, archiveType: ArchiveType) {
        FileOutputStream(archiveFilePath.toFile()).use { fileOutputStream ->
            when (archiveType) {
                ArchiveType.TAR_GZ, ArchiveType.TGZ, ArchiveType.TAR_BZ2, ArchiveType.TBZ2 -> {
                    CompressorStreamFactory().createCompressorOutputStream(
                        archiveType.compressorType,
                        fileOutputStream
                    ).use { compressorOutputStream ->
                        (ArchiveStreamFactory().createArchiveOutputStream(
                            archiveType.archiveType,
                            compressorOutputStream
                        ) as ArchiveOutputStream<ArchiveEntry>).use { archiveOutputStream ->
                            compressDirectory(sourcePath, sourcePath, archiveOutputStream)
                        }
                    }
                }

                else -> {
                    (ArchiveStreamFactory().createArchiveOutputStream(
                        archiveType.archiveType,
                        fileOutputStream
                    ) as ArchiveOutputStream<ArchiveEntry>).use { archiveOutputStream ->
                        compressDirectory(sourcePath, sourcePath, archiveOutputStream)
                    }
                }
            }
        }
    }

    private fun compressDirectory(
        rootPath: Path,
        currentPath: Path,
        archiveOutputStream: ArchiveOutputStream<ArchiveEntry>
    ) {
        Files.list(currentPath).use { files ->
            files.forEach { file ->
                val relativePath = rootPath.relativize(file)
                val entry = archiveOutputStream.createArchiveEntry(file.toFile(), relativePath.toString())
                archiveOutputStream.putArchiveEntry(entry)

                if (Files.isDirectory(file)) {
                    compressDirectory(rootPath, file, archiveOutputStream)
                } else {
                    FileInputStream(file.toFile()).use { inputStream ->
                        inputStream.copyTo(archiveOutputStream)
                    }

                    archiveOutputStream.closeArchiveEntry()
                }
            }
        }
    }

    private fun compressTo7z(sourcePath: Path, archiveFilePath: Path) {
        SevenZOutputFile(archiveFilePath.toFile()).use { sevenZOutput ->
            compressDirectoryTo7z(sourcePath, sourcePath, sevenZOutput)
        }
    }

    private fun compressDirectoryTo7z(
        rootPath: Path,
        currentPath: Path,
        sevenZOutput: SevenZOutputFile
    ) {
        Files.list(currentPath).use { files ->
            files.forEach { file ->
                val relativePath = rootPath.relativize(file).toString()
                addToArchiveCompression(sevenZOutput, file.toFile(), relativePath)
            }
        }
    }

    private fun addToArchiveCompression(out: SevenZOutputFile, file: File, dir: String) {
        val entryName = if (file.isDirectory) "$dir/" else dir // Add trailing slash for directories
        val entry = out.createArchiveEntry(file, entryName)
        out.putArchiveEntry(entry)

        if (file.isFile) {
            FileInputStream(file).use { inputStream ->
                val b = ByteArray(1024)
                var count: Int
                while (inputStream.read(b).also { count = it } > 0) {
                    out.write(b, 0, count)
                }
            }
            out.closeArchiveEntry()

        } else if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                addToArchiveCompression(out, child, "$dir/${child.name}")
            }
        } else {
            println("${file.name} is not supported")
        }
    }

    private fun getArchiveTypeFromExtension(extension: String): ArchiveType {
        val normalizedExtension = (if (extension.isNotEmpty() && extension[0] != '.') {
            ".$extension"
        } else {
            extension
        }).lowercase(Locale.getDefault())
        return ArchiveType.entries.firstOrNull { it.extension == normalizedExtension }
            ?: throw IllegalArgumentException("Unsupported archive format: $extension")
    }
}