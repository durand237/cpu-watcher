package com.example.cpuwatcher.monitoring.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.Instant
import com.example.cpuwatcher.monitoring.domain.ProcessMetric
import com.example.cpuwatcher.monitoring.domain.ProcessMetricBatch
import com.example.cpuwatcher.monitoring.domain.HostMetrics
import com.example.cpuwatcher.monitoring.application.ProcessMetricSearchPage

data class ProcessMetricBatchRequest(
	@field:NotBlank
	@field:Size(max = 255)
	val hostName: String,
	val collectedAt: Instant,
	@field:NotEmpty
	@field:Valid
	val processes: List<ProcessMetricRequest>,
	@field:Valid
	val hostMetrics: HostMetricsRequest,
)

data class HostMetricsRequest(
	@field:DecimalMin("0.0")
	@field:DecimalMax("100.0")
	val cpuUsagePercent: Double,
	@field:DecimalMin("0.0")
	@field:DecimalMax("100.0")
	val memoryUsagePercent: Double,
	@field:DecimalMin("0.0")
	@field:DecimalMax("100.0")
	val diskUsagePercent: Double,
)

data class ProcessMetricRequest(
	@field:PositiveOrZero
	val processId: Long,
	@field:NotBlank
	@field:Size(max = 512)
	val processName: String,
	@field:DecimalMin("0.0")
	@field:DecimalMax("100.0")
	val cpuUsagePercent: Double,
	@field:PositiveOrZero
	val memoryBytes: Long,
	@field:DecimalMin("0.0")
	@field:DecimalMax("100.0")
	val memoryUsagePercent: Double,
)

data class ProcessMetricBatchResponse(
	val accepted: Int,
)

data class ProcessMetricSnapshotResponse(
	val hostName: String,
	val collectedAt: Instant,
	val hostMetrics: HostMetricsResponse,
	val processes: List<ProcessMetricResponse>,
)

data class HostMetricsResponse(
	val cpuUsagePercent: Double,
	val memoryUsagePercent: Double,
	val diskUsagePercent: Double,
)

data class ProcessMetricResponse(
	val processId: Long,
	val processName: String,
	val cpuUsagePercent: Double,
	val memoryBytes: Long,
	val memoryUsagePercent: Double,
)

data class ProcessMetricOccurrenceResponse(
	val hostName: String,
	val collectedAt: Instant,
	val processId: Long,
	val processName: String,
	val cpuUsagePercent: Double,
	val memoryBytes: Long,
	val memoryUsagePercent: Double,
)

data class ProcessMetricOccurrencePageResponse(
	val occurrences: List<ProcessMetricOccurrenceResponse>,
	val page: Int,
	val size: Int,
	val totalElements: Long,
	val totalPages: Int,
)

fun ProcessMetricBatch.toSnapshotResponse(): ProcessMetricSnapshotResponse = ProcessMetricSnapshotResponse(
	hostName = hostName,
	collectedAt = collectedAt,
	hostMetrics = hostMetrics.toResponse(),
	processes = processes.map(ProcessMetric::toResponse),
)

private fun HostMetrics.toResponse(): HostMetricsResponse = HostMetricsResponse(
	cpuUsagePercent = cpuUsagePercent,
	memoryUsagePercent = memoryUsagePercent,
	diskUsagePercent = diskUsagePercent,
)

private fun ProcessMetric.toResponse(): ProcessMetricResponse = ProcessMetricResponse(
	processId = processId,
	processName = processName,
	cpuUsagePercent = cpuUsagePercent,
	memoryBytes = memoryBytes,
	memoryUsagePercent = memoryUsagePercent,
)

fun ProcessMetric.toOccurrenceResponse(): ProcessMetricOccurrenceResponse = ProcessMetricOccurrenceResponse(
	hostName = hostName,
	collectedAt = collectedAt,
	processId = processId,
	processName = processName,
	cpuUsagePercent = cpuUsagePercent,
	memoryBytes = memoryBytes,
	memoryUsagePercent = memoryUsagePercent,
)

fun ProcessMetricSearchPage.toOccurrencePageResponse(): ProcessMetricOccurrencePageResponse = ProcessMetricOccurrencePageResponse(
	occurrences = occurrences.map(ProcessMetric::toOccurrenceResponse),
	page = page,
	size = size,
	totalElements = totalElements,
	totalPages = totalPages,
)
