package app.simplecloud.droplet.serverhost.shared.logs

import app.simplecloud.controller.shared.time.ProtoBufTimestamp
import build.buf.gen.simplecloud.controller.v1.ServerHostStreamServerLogsResponse
import io.grpc.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit


class LogStreamer(private val logFile: Path, private val logger: Logger) {

    // Stream the output of the screen session's output (InputStream)
    fun readScreenLogs(): Flow<ServerHostStreamServerLogsResponse> {
        return flow {

            val ctx = Context.current()
            val deadline = ctx.deadline

            if (deadline == null) {
                // No deadline set, continue without worry
                logger.info("No deadline from client, continuing indefinitely")
            } else {
                // If there's a deadline, handle the operation based on it (but don't cancel the stream)
                logger.warn("Deadline set by client: ${deadline.timeRemaining(TimeUnit.MILLISECONDS)} milliseconds")
            }

            val file = RandomAccessFile(logFile.toFile(), "r")
            try {

                val channel = file.channel
                var lastPosition = 0L // Start at the end of the file

                while (currentCoroutineContext().isActive) {
                    // Wait for new data to be appended
                    val available = channel.size() - lastPosition

                    if (available > 0) {
                        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, lastPosition, available)
                        val data = Charset.defaultCharset().decode(buffer).toString()
                        val lines = data.split("\n")

                        for (line in lines) {
                            emit(
                                ServerHostStreamServerLogsResponse.newBuilder()
                                    .setContent(line)
                                    .setTimestamp(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                                    .build()
                            )
                        }
                        lastPosition += available
                    }

                    delay(1000L) // Check every second for new data
                }

            }catch (e: Exception) {
                logger.warn("Cancellation received from client: ", e)
            }finally {
                file.close()
                logger.info("Stopped streaming logs")
            }
        }.flowOn(Dispatchers.IO)
    }
}