package app.simplecloud.droplet.serverhost.runtime.runner

object ServerRunnerPlaceholders {
    val RUNNING_PATH = System.getenv("RUNNING_PATH") ?: "running"
}