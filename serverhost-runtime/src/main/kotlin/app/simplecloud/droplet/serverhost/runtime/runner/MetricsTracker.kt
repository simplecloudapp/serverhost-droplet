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


class MetricsTracker(
    private val pubSubClient: PubSubClient
) {

    private val logger = LogManager.getLogger(MetricsTracker::class.java)

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
            logger.warn("Could not track metrics", e)
        }
    }

    fun trackRamAndCpu(server: Server, process: ProcessHandle) {
        try {
            createMetricRamAndCpu(server, process).forEach { usageMetric ->
                pubSubClient.publish(MetricsEventNames.RECORD_METRIC, usageMetric)
            }
        } catch (e: Exception) {
            logger.warn("Could not track metrics", e)
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
        val cmd = Runtime.getRuntime()
            .exec(arrayOf("ps", "-wo", "%cpu,%mem", "--no-headers", "-p", process.pid().toString()))
        val result = cmd.inputReader(Charsets.UTF_8).readLine().split("   ")
        val cpu = result[0]
        val mem = result[1]
        return listOf(metric {
            metricType = "SERVER_USAGE_CPU"
            metricValue = cpu.toLong()
            time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            meta.addAll(createMeta(server))
        }, metric {
            metricType = "SERVER_USAGE_RAM"
            metricValue = mem.toLong()
            time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            meta.addAll(createMeta(server))
        })
    }

}