package app.simplecloud.droplet.serverhost.shared.hack

import app.simplecloud.droplet.serverhost.shared.process.ProcessInfo

class ScreenExecutor(private val pid: Long) {
    private val screenSession: ScreenSession? = findScreenSession(pid)

    fun isScreen(): Boolean = screenSession != null

    fun sendCommand(toSend: Array<String>) {
        val session = screenSession ?: return
        executeScreenCommand(session, toSend)
    }

    fun terminate(): Boolean {
        terminateChildProcesses(pid)
        if (screenSession == null) return true

        if (!terminateScreenSession()) return false
        return !checkSession(screenSession)
    }

    private fun terminateScreenSession(): Boolean {
        val session = screenSession ?: return true
        val quitProcess = ProcessBuilder("screen", "-S", session.name, "-X", "quit")
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        quitProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
        Thread.sleep(1000)

        if (!checkSession(session)) return true

        val killProcess = ProcessBuilder("screen", "-S", session.name, "-X", "kill")
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        killProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
        Thread.sleep(1000)

        if (!checkSession(session)) return true

        val process = ProcessHandle.of(session.pid).orElse(null) ?: return false
        process.destroy()
        Thread.sleep(500)

        if (!checkSession(session)) return true

        process.destroyForcibly()
        Thread.sleep(500)

        return true
    }

    private fun terminateChildProcesses(pid: Long) {
        val process = ProcessHandle.of(pid).orElse(null) ?: return
        process.descendants().forEach { descendant ->
            descendant.destroy()
            Thread.sleep(100)
            if (descendant.isAlive) {
                descendant.destroyForcibly()
                Thread.sleep(100)
            }
        }
    }

    private fun checkSession(session: ScreenSession): Boolean {
        val process = ProcessBuilder("screen", "-S", session.name, "-Q", "select", ".")
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return true
        }

        return process.exitValue() == 0
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
