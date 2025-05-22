package app.simplecloud.droplet.serverhost.runtime.files

import build.buf.gen.simplecloud.controller.v1.TemplateFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.logging.log4j.LogManager
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.*
import kotlin.io.path.*
import kotlin.time.Duration.Companion.milliseconds

class FileSystemSnapshotCache(
    private val directory: Path,
    debounceTimeMs: Long = 300
) {
    private val watchService = FileSystems.getDefault().newWatchService()
    private val logger = LogManager.getLogger(FileSystemSnapshotCache::class.java)
    private var files = mutableListOf<TemplateFile>()

    private var currentSnapshot: String? = null
    private var nextSnapshot: String? = UUID.randomUUID().toString()

    private val _events = MutableSharedFlow<FileSystemEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    @OptIn(FlowPreview::class)
    val events = _events.asSharedFlow()
        .filter { event -> event is FileSystemEvent.SystemChanged }
        .debounce(debounceTimeMs.milliseconds)

    fun registerWatcher(): Job {
        registerDirectoryAndSubdirectories(directory)

        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val key = watchService.take()
                    var changed = false
                    val processedPaths = mutableSetOf<Path>()

                    for (event in key.pollEvents()) {
                        val path = event.context() as? Path ?: continue
                        val watchedDir = key.watchable() as? Path ?: continue
                        val resolvedPath = watchedDir.resolve(path)

                        if (!processedPaths.add(resolvedPath)) continue

                        logger.debug("Detected change in $resolvedPath")
                        changed = true

                        when (event.kind()) {
                            StandardWatchEventKinds.ENTRY_CREATE -> {
                                if (resolvedPath.isDirectory()) {
                                    registerDirectoryAndSubdirectories(resolvedPath)
                                    _events.emit(FileSystemEvent.DirectoryCreated(resolvedPath))
                                } else {
                                    _events.emit(FileSystemEvent.FileCreated(resolvedPath))
                                }
                            }
                            StandardWatchEventKinds.ENTRY_MODIFY -> {
                                if (!resolvedPath.isDirectory()) {
                                    _events.emit(FileSystemEvent.FileModified(resolvedPath))
                                }
                            }
                            StandardWatchEventKinds.ENTRY_DELETE -> {
                                if (resolvedPath.isDirectory()) {
                                    _events.emit(FileSystemEvent.DirectoryDeleted(resolvedPath))
                                } else {
                                    _events.emit(FileSystemEvent.FileDeleted(resolvedPath))
                                }
                            }
                        }

                        nextSnapshot = UUID.randomUUID().toString()
                    }

                    if (changed) {
                        _events.emit(FileSystemEvent.SystemChanged)
                    }

                    key.reset()
                } catch (e: Exception) {
                    logger.error("Error in file watcher", e)
                    delay(1000)
                }
            }
        }
    }

    private fun registerDirectoryAndSubdirectories(dir: Path) {
        if (!dir.isDirectory()) return

        try {
            logger.debug("Registering directory for watching: $dir")
            dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )

            dir.listDirectoryEntries().forEach { entry ->
                if (entry.isDirectory()) {
                    registerDirectoryAndSubdirectories(entry)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to register directory for watching: $dir", e)
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
