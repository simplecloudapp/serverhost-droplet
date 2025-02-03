package app.simplecloud.droplet.serverhost.runtime.runner

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.api.metrics.MetricsEventNames
import app.simplecloud.droplet.api.time.ProtobufTimestamp
import app.simplecloud.droplet.serverhost.shared.process.ProcessInfo
import app.simplecloud.pubsub.PubSubClient
import build.buf.gen.simplecloud.metrics.v1.Metric
import build.buf.gen.simplecloud.metrics.v1.MetricMeta
import build.buf.gen.simplecloud.metrics.v1.metric
import build.buf.gen.simplecloud.metrics.v1.metricMeta
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime


class MetricsTracker(
    private val pubSubClient: PubSubClient,
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
            time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            meta.addAll(createMeta(server))
        }
    }

    private fun createMetricRamAndCpu(server: Server, process: ProcessHandle): List<Metric> {
        val ramAndCpu = ProcessInfo.of(process).getRamAndCpuPercent()
        return listOf(metric {
            metricType = "SERVER_USAGE_CPU"
            metricValue = (ramAndCpu.first * 1000L).toLong()
            time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            meta.addAll(createMeta(server))
        }, metric {
            metricType = "SERVER_USAGE_RAM"
            metricValue = (ramAndCpu.second * 1000L).toLong()
            time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            meta.addAll(createMeta(server))
        })
    }

}