package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.files.FileSystemSnapshotCache
import app.simplecloud.droplet.serverhost.runtime.launcher.ServerHostStartCommand
import app.simplecloud.droplet.serverhost.runtime.runner.ServerEnvironments
import app.simplecloud.droplet.serverhost.shared.hack.PortProcessHandle
import build.buf.gen.simplecloud.controller.v1.*
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.*

class ServerHostService(
    private val serverHost: ServerHost,
    private val envs: ServerEnvironments,
    private val cache: FileSystemSnapshotCache,
    private val args: ServerHostStartCommand,
) : ServerHostServiceGrpcKt.ServerHostServiceCoroutineImplBase() {

    override suspend fun startServer(request: ServerHostStartServerRequest): ServerDefinition {
        val group = Group.fromDefinition(request.group)
        val port = PortProcessHandle.findNextFreePort(group.startPort.toInt(), request.server)
        val server = Server.fromDefinition(request.server.copy {
            this.serverState = ServerState.STARTING
            this.serverPort = port.toLong()
            this.hostId = serverHost.id
            this.serverIp = serverHost.host
        })
        try {
            val env = envs.firstFor(server)
            val started = env.startServer(server)
            if (!started) {
                throw StatusException(Status.INVALID_ARGUMENT.withDescription("Group not supported by this ServerHost."))
            }
            return server.toDefinition()
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Failed to start server:").withCause(e))
        }
    }

    override suspend fun stopServer(request: ServerDefinition): ServerDefinition {
        val env =
            envs.of(request.uniqueId) ?: throw StatusException(Status.NOT_FOUND.withDescription("Server not found"))
        val stopped = env.stopServer(Server.fromDefinition(request))
        if (!stopped) {
            throw StatusException(Status.INTERNAL.withDescription("Could not stop server"))
        }
        return request
    }

    override suspend fun reattachServer(request: ServerDefinition): ServerDefinition {
        val server = Server.fromDefinition(request)
        val env =
            envs.firstFor(server)
        val success = env.reattachServer(server)
        if (!success) {
            throw StatusException(Status.INTERNAL.withDescription("Could not reattach server"))
        }
        return request
    }

    override suspend fun executeCommand(request: ServerHostServerExecuteCommandRequest): ServerHostServerExecuteCommandResponse {
        val env =
            envs.of(request.serverId) ?: throw StatusException(Status.NOT_FOUND.withDescription("Server not found"))
        val success = env.executeCommand(env.getServer(request.serverId)!!, request.command)
        if (!success) throw StatusException(Status.INTERNAL.withDescription("Could not send command."))
        return serverHostServerExecuteCommandResponse {}
    }

    override fun streamServerLogs(request: ServerHostStreamServerLogsRequest): Flow<ServerHostStreamServerLogsResponse> {
        val env =
            envs.of(request.serverId) ?: throw StatusException(Status.NOT_FOUND.withDescription("Server not found"))
        try {
            return env.streamLogs(env.getServer(request.serverId)!!)
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Could not stream logs: ${e.message}"))
        }
    }

    override suspend fun getFileContents(request: GetFileContentsRequest): GetFileContentsResponse {
        val file = Paths.get(args.templatePath.absolutePathString(), request.path)
        if (!file.exists() || file.isDirectory())
            throw StatusException(Status.NOT_FOUND.withDescription("File not found or a directory"))
        val fileData = ByteArray(file.toFile().length().toInt())
        withContext(Dispatchers.IO) {
            val inputStream = FileInputStream(file.toFile())
            inputStream.read(fileData)
            inputStream.close()
        }
        return getFileContentsResponse {
            content = ByteString.copyFrom(fileData)
        }
    }

    override suspend fun getFileTree(request: GetFileTreeRequest): GetFileTreeResponse {
        cache.update()

        return getFileTreeResponse {
            files.addAll(cache.get())
        }
    }

    @OptIn(ExperimentalPathApi::class)
    override suspend fun deleteFile(request: DeleteFileRequest): DeleteFileResponse {
        val file = Paths.get(args.templatePath.absolutePathString(), request.path)
        if (!file.exists())
            throw StatusException(Status.NOT_FOUND.withDescription("File not found"))
        if (!file.isDirectory())
            file.deleteIfExists()
        else
            file.deleteRecursively()
        return deleteFileResponse {}
    }

    override suspend fun moveFile(request: MoveFileRequest): MoveFileResponse {
        val from = Paths.get(args.templatePath.absolutePathString(), request.from)
        if (!from.exists())
            throw StatusException(Status.NOT_FOUND.withDescription("From file not found"))
        val to = Paths.get(args.templatePath.absolutePathString(), request.to)
        withContext(Dispatchers.IO) {
            Files.move(from, to)
        }
        return moveFileResponse {
        }
    }

    override suspend fun updateFile(request: UpdateFileRequest): UpdateFileResponse {
        val file = Paths.get(args.templatePath.absolutePathString(), request.path)
        if (!file.exists()) {
            file.parent.createDirectories()
            file.createFile()
        }

        withContext(Dispatchers.IO) {
            val writer = FileWriter(file.toFile(), Charsets.UTF_8)
            writer.write(request.content.toStringUtf8())
            writer.flush()
            writer.close()
        }

        return updateFileResponse {

        }
    }


}