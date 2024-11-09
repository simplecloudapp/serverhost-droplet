package app.simplecloud.droplet.serverhost.shared.logs

import app.simplecloud.controller.shared.time.ProtoBufTimestamp
import build.buf.gen.simplecloud.controller.v1.ServerHostStreamServerLogsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.FileInputStream
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.*


class LogStreamer(private val logFile: Path) {

    private var streaming = false

    init {
        streaming = true
    }

    // Stream the output of the screen session's output (InputStream)
    fun readScreenLogs(): Flow<ServerHostStreamServerLogsResponse> {
        return flow {
            val reader = Scanner(FileInputStream(logFile.toFile()))
            var line: String?
            println("started logging")
            while (reader.hasNextLine().also { line = reader.nextLine() }) {
                println(line)
                emit(
                    ServerHostStreamServerLogsResponse.newBuilder().setContent(line ?: "").setTimestamp(
                        ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now())
                    ).build()
                )
            }
            stopHook()
        }.flowOn(Dispatchers.IO)
    }

    fun stopHook() {
        println("stopped logging")
        streaming = false;
    }
}