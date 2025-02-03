package app.simplecloud.droplet.serverhost.shared.logs

import build.buf.gen.simplecloud.controller.v1.ServerHostStreamServerLogsResponse
import kotlinx.coroutines.flow.Flow

interface LogStreamer {
    fun readScreenLogs(): Flow<ServerHostStreamServerLogsResponse>
}