package app.simplecloud.droplet.serverhost.runtime.files

import build.buf.gen.simplecloud.controller.v1.TemplateFile
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.*
import kotlin.io.path.*

class FileSystemSnapshotCache(private val directory: Path) {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val logger = LogManager.getLogger(FileSystemSnapshotCache::class.java)
    private var files = mutableListOf<TemplateFile>()

    private var currentSnapshot: String? = null
    private var nextSnapshot: String? = UUID.randomUUID().toString()

    fun registerWatcher(): Job {
        directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val key = watchService.take()
                for (event in key.pollEvents()) {
                    val path = event.context() as? Path ?: continue
                    val resolvedPath = directory.resolve(path)
                    logger.info("Detected change in $resolvedPath")
                    nextSnapshot = UUID.randomUUID().toString()
                }
                key.reset()
            }
        }
    }

    fun update() {
        if (currentSnapshot != nextSnapshot) {
            create()
            currentSnapshot = nextSnapshot
        }
    }

    private fun create() {
        files.clear()
        collectFiles(directory, files)
    }

    private fun collectFiles(current: Path, list: MutableList<TemplateFile>) {
        if (!current.exists()) return
        if (current.isDirectory()) {
            list.add(
                TemplateFile.newBuilder().setType("directory").setIsDirectory(true)
                    .setPath(directory.relativize(current).pathString).build()
            )
            current.listDirectoryEntries().forEach { collectFiles(it, list) }
            return
        }
        list.add(
            TemplateFile.newBuilder().setType(current.extension.replace("yml", "yaml"))
                .setPath(directory.relativize(current).pathString)
                .setIsDirectory(false).build()
        )
    }

    fun get(): List<TemplateFile> {
        return files
    }
}