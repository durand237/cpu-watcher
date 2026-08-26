package com.example.cpuwatcher.collector

import oshi.SystemInfo
import oshi.software.os.OSProcess
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.InetAddress
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

data class ProcessMetricBatchPayload(
	val hostName: String,
	val collectedAt: Instant,
	val processes: List<ProcessMetricPayload>,
	val hostMetrics: HostMetricsPayload,
)

data class HostMetricsPayload(
	val cpuUsagePercent: Double,
	val memoryUsagePercent: Double,
	val diskUsagePercent: Double,
)

data class ProcessMetricPayload(
	val processId: Long,
	val processName: String,
	val cpuUsagePercent: Double,
	val memoryBytes: Long,
	val memoryUsagePercent: Double,
)

@Component
class HostMetricsCollector(
	private val systemInfo: SystemInfo,
	private val restClient: RestClient,
	@Value("\${collector.api-key:}") private val apiKey: String,
	@Value("\${collector.max-processes:0}") private val maxProcesses: Int,
) {
	private val logger = LoggerFactory.getLogger(javaClass)
	private val previousSnapshots = ConcurrentHashMap<Int, OSProcess>()
	private val operatingSystem = systemInfo.operatingSystem
	private val totalMemoryBytes = systemInfo.hardware.memory.total
	private val logicalProcessorCount = max(1, systemInfo.hardware.processor.logicalProcessorCount)
	private val processor = systemInfo.hardware.processor
	private var previousCpuTicks = processor.systemCpuLoadTicks
	private val hostName = resolveHostName()

	@Scheduled(
		fixedDelayString = "\${collector.interval-ms:5000}",
		initialDelayString = "\${collector.initial-delay-ms:5000}",
	)
	fun collectAndSend() {
		if (apiKey.isBlank()) {
			logger.warn("COLLECTOR_API_KEY is not configured; metrics will not be sent")
			return
		}

		try {
			val processes = if (maxProcesses > 0) {
				operatingSystem.getProcesses(null, null, maxProcesses)
			} else {
				operatingSystem.getProcesses()
			}
			val currentProcessIds = processes.mapTo(mutableSetOf()) { it.processID }
			val collectedAt = Instant.now()

			val payload = ProcessMetricBatchPayload(
				hostName = hostName,
				collectedAt = collectedAt,
				processes = processes.map { process ->
					val priorSnapshot = previousSnapshots.put(process.processID, process)
					val cpuLoad = priorSnapshot?.let { process.getProcessCpuLoadBetweenTicks(it) } ?: 0.0
					val cpuPercent = (cpuLoad * 100.0 / logicalProcessorCount).coerceIn(0.0, 100.0)
					val memoryBytes = process.privateResidentMemory.coerceAtLeast(0L)
					val memoryPercent = if (totalMemoryBytes > 0) {
						(memoryBytes.toDouble() * 100.0 / totalMemoryBytes).coerceIn(0.0, 100.0)
					} else {
						0.0
					}

					ProcessMetricPayload(
						processId = process.processID.toLong(),
						processName = process.name.orEmpty().take(512),
						cpuUsagePercent = cpuPercent,
						memoryBytes = memoryBytes,
						memoryUsagePercent = memoryPercent,
					)
				},
				hostMetrics = HostMetricsPayload(
					cpuUsagePercent = collectCpuUsagePercent(),
					memoryUsagePercent = collectMemoryUsagePercent(),
					diskUsagePercent = collectDiskUsagePercent(),
				),
			)

			previousSnapshots.keys.retainAll(currentProcessIds)
			restClient.post()
				.uri("/api/v1/metrics/processes")
				.header("X-Collector-Api-Key", apiKey)
				.body(payload)
				.retrieve()
				.toBodilessEntity()
		} catch (exception: Exception) {
			logger.warn("Could not collect or send host metrics: {}", exception.message)
		}
	}

	private fun resolveHostName(): String = sequenceOf(
		System.getenv("COMPUTERNAME"),
		System.getenv("HOSTNAME"),
		runCatching { InetAddress.getLocalHost().hostName }.getOrNull(),
	).filterNot { it.isNullOrBlank() }.firstOrNull() ?: "unknown-host"

	private fun collectCpuUsagePercent(): Double {
		val usage = (processor.getSystemCpuLoadBetweenTicks(previousCpuTicks) * 100.0).coerceIn(0.0, 100.0)
		previousCpuTicks = processor.systemCpuLoadTicks
		return usage
	}

	private fun collectMemoryUsagePercent(): Double {
		if (totalMemoryBytes <= 0) return 0.0
		val usedMemoryBytes = (totalMemoryBytes - systemInfo.hardware.memory.available).coerceAtLeast(0)
		return (usedMemoryBytes.toDouble() * 100.0 / totalMemoryBytes).coerceIn(0.0, 100.0)
	}

	private fun collectDiskUsagePercent(): Double {
		val fileStores = operatingSystem.fileSystem.fileStores
		val totalBytes = fileStores.sumOf { it.totalSpace.coerceAtLeast(0) }
		if (totalBytes <= 0) return 0.0
		val usableBytes = fileStores.sumOf { it.usableSpace.coerceAtLeast(0) }.coerceAtMost(totalBytes)
		return ((totalBytes - usableBytes).toDouble() * 100.0 / totalBytes).coerceIn(0.0, 100.0)
	}
}
