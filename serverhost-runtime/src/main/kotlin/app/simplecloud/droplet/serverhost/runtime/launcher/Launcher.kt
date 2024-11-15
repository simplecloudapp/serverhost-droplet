package app.simplecloud.droplet.serverhost.runtime.launcher

import app.simplecloud.metrics.internal.api.MetricsCollector
import com.github.ajalt.clikt.command.main
import org.apache.logging.log4j.LogManager

suspend fun main(args: Array<String>) {
    val metricsCollector = try {
        MetricsCollector.create("serverhost-droplet")
    } catch (e: Exception) {
        null
    }
    println(metricsCollector)
    configureLog4j(metricsCollector)
    ServerHostStartCommand(metricsCollector).main(args)
}

fun configureLog4j(metricsCollector: MetricsCollector?) {
    val globalExceptionHandlerLogger = LogManager.getLogger("GlobalExceptionHandler")
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        metricsCollector?.recordError(throwable)
        globalExceptionHandlerLogger.error("Uncaught exception in thread ${thread.name}", throwable)
    }
}