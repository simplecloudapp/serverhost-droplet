package app.simplecloud.droplet.serverhost.runtime.host

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.serverhost.runtime.environment.ServerEnvironment
import app.simplecloud.droplet.serverhost.runtime.environment.ServerEnvironments
import app.simplecloud.droplet.serverhost.shared.hack.PortProcessHandle
import build.buf.gen.simplecloud.controller.v1.ServerDefinition
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.copy
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.minutes

class ServerOperationReconciler(
    private val serverHost: ServerHost,
    private val envs: ServerEnvironments,
    private val maxConcurrentOperations: Int,
) {

    private val logger = LogManager.getLogger(ServerOperationReconciler::class.java)

    sealed class Operation {
        data class Start(
            val server: ServerDefinition,
            val group: Group,
            val deferred: CompletableDeferred<ServerDefinition>
        ) : Operation()

        // Maybe we will add an update operation later
    }

    private val pendingOperations = ConcurrentLinkedQueue<Operation>()
    private val startingServers = ConcurrentHashMap<String, Job>()

    fun start() {
        logger.info("Starting ServerOperationReconciler with max concurrent operations: {}", maxConcurrentOperations)
        CoroutineScope(Dispatchers.IO).launch {
            reconcile()
        }
    }

    suspend fun submitStart(server: ServerDefinition, group: Group): ServerDefinition {
        logger.info("Submitting start operation for server: {}", server.uniqueId)
        val deferred = CompletableDeferred<ServerDefinition>()
        pendingOperations.offer(Operation.Start(server, group, deferred))
        return deferred.await()
    }
    private suspend fun reconcile() = coroutineScope {
        while (isActive) {
            cleanupCompletedServers()

            if (startingServers.size >= maxConcurrentOperations) {
                continue
            }

            val operation = pendingOperations.poll()?: continue

            when (operation) {
                is Operation.Start -> {
                    logger.info("Processing start operation for server: {}", operation.server.uniqueId)
                    val startJob = launch {
                        try {
                            val startedServer = startServer(operation)
                            operation.deferred.complete(startedServer)
                            waitForServerOnline(startedServer)
                        } catch (e: Exception) {
                            logger.info("Start operation failed for server: {} - {}", operation.server.uniqueId, e.message)
                            operation.deferred.completeExceptionally(e)
                        }
                    }
                    startingServers[operation.server.uniqueId] = startJob
                }
            }
            delay(100)
        }
    }

    private fun cleanupCompletedServers() {
        startingServers.entries.removeIf { (_, job) -> job.isCompleted }
    }

    private suspend fun waitForServerOnline(server: ServerDefinition) {
        logger.info("Waiting for server to come online: {}", server.uniqueId)
        val env = envs.of(server.uniqueId) ?: return

        withTimeoutOrNull(2.minutes) {
            while (true) {
                if (isServerOnline(env, server)) {
                    logger.info("Server is now online: {}", server.uniqueId)
                    return@withTimeoutOrNull
                }
                delay(500)
            }
        } ?: logger.warn("Timeout waiting for server to come online: {}", server.uniqueId)
    }

    private fun isServerOnline(env: ServerEnvironment, server: ServerDefinition): Boolean {
        return try {
            val currentServer = env.getServer(server.uniqueId) ?: return false
            currentServer.state == ServerState.AVAILABLE || currentServer.state == ServerState.INGAME
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun startServer(operation: Operation.Start): ServerDefinition {
        val port = PortProcessHandle.findNextFreePort(operation.group.startPort.toInt(), operation.server)
        logger.info("Allocated port {} for server: {}", port, operation.server.uniqueId)

        val server = operation.server.copy {
            serverState = ServerState.STARTING
            serverPort = port.toLong()
            hostId = serverHost.id
            serverIp = serverHost.host
        }

        val env = envs.firstFor(Server.fromDefinition(server))
        logger.info("Starting server {} using environment", server.uniqueId)
        val started = env.startServer(Server.fromDefinition(server))

        if (!started) {
            logger.info("Failed to start server: {} - Group not supported", server.uniqueId)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Group not supported by this ServerHost."))
        }

        logger.info("Successfully started server: {}", server.uniqueId)
        return server
    }

}
