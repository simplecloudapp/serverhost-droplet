package app.simplecloud.droplet.serverhost.shared

import app.simplecloud.controller.shared.time.ProtoBufTimestamp
import build.buf.gen.simplecloud.controller.v1.ServerHostStreamServerLogsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.time.LocalDateTime

class ScreenLogStreamer(private val pid: Long) {

    private var streamingProcess: Process? = null

    fun isScreen(): Boolean {
        return getScreenSessionByPid() != null
    }

    private fun getScreenSessionByPid(): String? {
        // Find the screen session associated with the PID
        val command = "ps aux | grep SCREEN | grep $pid"
        val process = ProcessBuilder("bash", "-c", command).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        val sessionInfo = reader.readLine()
        return sessionInfo?.split(" ")?.get(1) // Extract session name (if available)
    }

    private fun startScreenProcess(): Process {
        val command = arrayOf("screen", "-S", getScreenSessionByPid(), "-X")
        val processBuilder = ProcessBuilder(*command)
        return processBuilder.start()
    }

    // Send a command to the screen session
    fun sendCommand(command: String) {
        val process = startScreenProcess()

        // Get the input stream and write the command to it
        val outputStream: OutputStream = process.outputStream
        outputStream.write("$command\n".toByteArray())
        outputStream.flush()
        stopHook(process)
    }

    // Stream the output of the screen session's output (InputStream)
    fun readScreenLogs(): Flow<ServerHostStreamServerLogsResponse> {
        streamingProcess = startScreenProcess()
        return flow {
            val reader = BufferedReader(InputStreamReader(streamingProcess!!.inputStream))
            var line: String?
            while (streamingProcess!!.isAlive) {
                if (reader.readLine().also { line = it } == null) continue
                emit(
                    ServerHostStreamServerLogsResponse.newBuilder().setContent(line ?: "").setTimestamp(
                        ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now())
                    ).build()
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    // Cancel the process explicitly if needed
    private fun stopHook(process: Process) {
        process.destroy()
    }

    fun stopHook() {
        stopHook(streamingProcess!!)
    }
}