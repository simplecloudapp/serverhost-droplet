package app.simplecloud.droplet.serverhost.shared.hack

import app.simplecloud.droplet.serverhost.shared.process.ProcessInfo
import kotlinx.coroutines.*

class ScreenExecutor(private val pid: Long) {

    private val screenSession: ScreenSession? = findScreenSession(pid)

    fun isScreen(): Boolean = screenSession != null

    fun sendCommand(toSend: Array<String>) {
        val session = screenSession ?: return
        executeScreenCommand(session, toSend)
    }

    suspend fun terminate(): Boolean {
        terminateChildProcesses(pid)
        if (screenSession == null) {
            return true
        }

        if (!terminateScreenSession()) {
            return false
        }

        return !checkSession(screenSession)
    }

    private suspend fun terminateScreenSession(): Boolean {
        val session = screenSession ?: return true

        if (executeScreenAction(session, "quit")) {
            return true
        }

        if (executeScreenAction(session, "kill")) {
            return true
        }

        val process = ProcessHandle.of(session.pid).orElse(null) ?: return false

        return forceTerminateProcess(process)
    }

    private suspend fun executeScreenAction(session: ScreenSession, action: String): Boolean {
        val process = ProcessBuilder("screen", "-S", session.name, "-X", action)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        return withTimeoutOrNull(2000) {
            val terminated = process.waitFor()
            if (terminated != 0) {
                process.destroyForcibly()
                return@withTimeoutOrNull false
            }

            delay(100)
            !checkSession(session)
        } ?: false
    }

    private suspend fun forceTerminateProcess(process: ProcessHandle): Boolean {
        process.destroy()

        if (waitForTermination(process, 60_000)) {
            return true
        }

        process.destroyForcibly()

        return waitForTermination(process, 10_000)
    }

    private suspend fun waitForTermination(process: ProcessHandle, timeoutMs: Long): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            while (process.isAlive) {
                delay(100)
            }
            true
        } ?: false
    }

    private suspend fun terminateChildProcesses(pid: Long) {
        val process = ProcessHandle.of(pid).orElse(null) ?: return
        coroutineScope {
            process.descendants()
                .toList()
                .map { descendant ->
                    async {
                        forceTerminateProcess(descendant)
                    }
                }
                .awaitAll()
        }
    }

    private suspend fun checkSession(session: ScreenSession): Boolean {
        val process = ProcessBuilder("screen", "-S", session.name, "-Q", "select", ".")
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        return withTimeoutOrNull(2000) {
            val exitValue = process.waitFor()
            exitValue == 0
        } ?: run {
            process.destroyForcibly()
            true
        }
    }

    private fun findScreenSession(pid: Long): ScreenSession? {
        var handle = ProcessHandle.of(pid)
        while (handle.isPresent) {
            val process = handle.get()
            val command = ProcessInfo.of(process).getCommand().lowercase()
            if (command.startsWith("screen")) {
                val sessionName = extractScreenSessionName(command) ?: process.pid().toString()
                return ScreenSession(process.pid(), sessionName)
            }
            handle = process.parent()
        }
        return null
    }

    private fun extractScreenSessionName(command: String): String? {
        val regex = "\\s-S\\s+([^\\s]+)".toRegex()
        return regex.find(command)?.groupValues?.get(1)
    }

    private fun executeScreenCommand(session: ScreenSession, toSend: Array<String>) {
        val command = arrayOf("screen", "-S", session.name, "-X") + toSend
        val process = ProcessBuilder(*command)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
    }

    private data class ScreenSession(val pid: Long, val name: String)

}
