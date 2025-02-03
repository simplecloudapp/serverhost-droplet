package app.simplecloud.droplet.serverhost.shared.actions.impl.codec

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorStreamFactory

enum class ArchiveType(val extension: String, val archiveType: String, val compressorType: String? = null) {

    TAR_GZ(".tar.gz", ArchiveStreamFactory.TAR, CompressorStreamFactory.GZIP),
    TGZ(".tgz", ArchiveStreamFactory.TAR, CompressorStreamFactory.GZIP),
    TAR_BZ2(".tar.bz2", ArchiveStreamFactory.TAR, CompressorStreamFactory.BZIP2),
    TBZ2(".tbz2", ArchiveStreamFactory.TAR, CompressorStreamFactory.BZIP2),
    ZIP(".zip", ArchiveStreamFactory.ZIP),
    TAR(".tar", ArchiveStreamFactory.TAR),
    SEVEN_Z(".7z", "7z")

}