package com.example.cpuwatcher.monitoring.domain

import java.time.Instant

data class ProcessMetric(
	val hostName: String,
	val collectedAt: Instant,
	val processId: Long,
	val processName: String,
	val cpuUsagePercent: Double,
	val memoryBytes: Long,
	val memoryUsagePercent: Double,
)

data class ProcessMetricBatch(
	val hostName: String,
	val collectedAt: Instant,
	val hostMetrics: HostMetrics,
	val processes: List<ProcessMetric>,
)

data class HostMetrics(
	val cpuUsagePercent: Double,
	val memoryUsagePercent: Double,
	val diskUsagePercent: Double,
)
