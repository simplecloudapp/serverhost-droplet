package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.api.metrics.MetricsEventNames
import app.simplecloud.droplet.api.time.ProtobufTimestamp
import app.simplecloud.pubsub.PubSubClient
import build.buf.gen.simplecloud.metrics.v1.Metric
import build.buf.gen.simplecloud.metrics.v1.MetricMeta
import build.buf.gen.simplecloud.metrics.v1.metric
import build.buf.gen.simplecloud.metrics.v1.metricMeta
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import java.util.*


class MetricsTracker(
    private val pubSubClient: PubSubClient
) {

    private val logger = LogManager.getLogger(MetricsTracker::class.java)

    private fun getHighestJavaProcess(handle: ProcessHandle): Optional<ProcessHandle> {
        var returned = Optional.empty<ProcessHandle>()
        handle.children().forEach { child ->
            if (!returned.isEmpty) return@forEach
            if (child.info().commandLine().orElseGet { "" }.lowercase().contains("java") && !child.info().commandLine()
                    .orElseGet { "" }.lowercase().startsWith("screen")
            )
                returned = Optional.of(child)
        }
        if (!returned.isEmpty) return returned
        handle.children().forEach { child ->
            if (!returned.isEmpty) return@forEach
            returned = getHighestJavaProcess(child)
        }
        return returned
    }

    private fun createMeta(server: Server): List<MetricMeta> {
        return listOf(
            metricMeta {
                dataName = "serverId"
                dataValue = server.uniqueId
            },
            metricMeta {
                dataName = "groupName"
                dataValue = server.group
            },
            metricMeta {
                dataName = "numericalId"
                dataValue = server.numericalId.toString()
            },

            )
    }

    fun trackPlayers(server: Server) {
        try {
            pubSubClient.publish(MetricsEventNames.RECORD_METRIC, createMetricPlayers(server))
        } catch (e: Exception) {
            logger.warn("Could not track metrics: ${e.message}")
        }
    }

    fun trackRamAndCpu(server: Server, process: ProcessHandle) {
        try {
            createMetricRamAndCpu(server, process).forEach { usageMetric ->
                pubSubClient.publish(MetricsEventNames.RECORD_METRIC, usageMetric)
            }
        } catch (e: Exception) {
            logger.warn("Could not track metrics: ${e.message}")
        }
    }


    private fun createMetricPlayers(server: Server): Metric {
        return metric {
            metricType = "SERVER_USAGE_PLAYERS"
            metricValue = server.playerCount
            meta.addAll(createMeta(server))
        }
    }

    private fun createMetricRamAndCpu(server: Server, process: ProcessHandle): List<Metric> {
        val javaProcess = getHighestJavaProcess(process).get()
        val cmd = Runtime.getRuntime()
            .exec(arrayOf("ps", "-wo", "%cpu,%mem", "--no-headers", "--forest", "-p", javaProcess.pid().toString()))
        var cpu = 0L
        var mem = 0L
        cmd.inputReader(Charsets.UTF_8).use { reader ->
            reader.readLines().forEach { line ->
                val resultString = line.trim()
                val result = resultString.split("  ")
                cpu = (result[0].toDouble() * 1000L).toLong()
                mem = (result[1].toDouble() * 1000L).toLong()
            }
        }
        return listOf(metric {
            metricType = "SERVER_USAGE_CPU"
            metricValue = cpu
            time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            meta.addAll(createMeta(server))
        }, metric {
            metricType = "SERVER_USAGE_RAM"
            metricValue = mem
            time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            meta.addAll(createMeta(server))
        })
    }

}