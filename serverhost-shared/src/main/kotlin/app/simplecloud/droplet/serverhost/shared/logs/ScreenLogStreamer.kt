package app.simplecloud.droplet.serverhost.shared.logs

import build.buf.gen.simplecloud.controller.v1.ServerHostStreamServerLogsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.file.Path

class ScreenLogStreamer(pid: Long, logFile: Path) : LogStreamer {
    private val configurer = ScreenConfigurer(pid)
    private val streamer = DefaultLogStreamer(logFile)

    override fun readScreenLogs(): Flow<ServerHostStreamServerLogsResponse> {
        configurer.setLogsFlush(0)
        return flow {
            emitAll(streamer.readScreenLogs())
            configurer.setLogsFlush(10)
        }.flowOn(Dispatchers.IO)
    }

    fun close() {
        configurer.setLogsFlush(10)
    }


}