package app.simplecloud.droplet.serverhost.runtime.files

import java.nio.file.Path

sealed class FileSystemEvent {
    data class FileCreated(val path: Path) : FileSystemEvent()
    data class FileModified(val path: Path) : FileSystemEvent()
    data class FileDeleted(val path: Path) : FileSystemEvent()
    data class DirectoryCreated(val path: Path) : FileSystemEvent()
    data class DirectoryDeleted(val path: Path) : FileSystemEvent()

    data object SystemChanged : FileSystemEvent()
}
