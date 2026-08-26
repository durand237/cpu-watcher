package com.example.cpuwatcher.monitoring.api

import com.example.cpuwatcher.monitoring.api.dto.ProcessMetricBatchRequest
import com.example.cpuwatcher.monitoring.api.dto.ProcessMetricBatchResponse
import com.example.cpuwatcher.monitoring.api.dto.ProcessMetricSnapshotResponse
import com.example.cpuwatcher.monitoring.api.dto.toSnapshotResponse
import com.example.cpuwatcher.monitoring.api.dto.toOccurrenceResponse
import com.example.cpuwatcher.monitoring.api.dto.toOccurrencePageResponse
import com.example.cpuwatcher.monitoring.application.ProcessMetricService
import com.example.cpuwatcher.monitoring.domain.ProcessMetric
import com.example.cpuwatcher.monitoring.domain.ProcessMetricBatch
import com.example.cpuwatcher.monitoring.domain.HostMetrics
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/metrics")
class ProcessMetricController(
	private val service: ProcessMetricService,
	private val sseRegistry: ProcessMetricSseRegistry,
) {

	@PostMapping("/processes")
	@ResponseStatus(HttpStatus.ACCEPTED)
	fun ingest(@Valid @RequestBody request: ProcessMetricBatchRequest): ProcessMetricBatchResponse {
		val batch = ProcessMetricBatch(
			hostName = request.hostName,
			collectedAt = request.collectedAt,
			hostMetrics = HostMetrics(
				cpuUsagePercent = request.hostMetrics.cpuUsagePercent,
				memoryUsagePercent = request.hostMetrics.memoryUsagePercent,
				diskUsagePercent = request.hostMetrics.diskUsagePercent,
			),
			processes = request.processes.map { metric ->
				ProcessMetric(
					hostName = request.hostName,
					collectedAt = request.collectedAt,
					processId = metric.processId,
					processName = metric.processName,
					cpuUsagePercent = metric.cpuUsagePercent,
					memoryBytes = metric.memoryBytes,
					memoryUsagePercent = metric.memoryUsagePercent,
				)
			},
		)

		return ProcessMetricBatchResponse(service.ingest(batch))
	}

	@GetMapping("/processes/latest")
	fun latestSnapshot(@RequestParam(required = false) hostName: String?): ProcessMetricSnapshotResponse =
		service.findLatestSnapshot(hostName?.let(::requireHostName))?.toSnapshotResponse()
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No metrics found for host")

	@GetMapping("/processes/search")
	fun searchProcessOccurrences(
		@RequestParam hostName: String,
		@RequestParam(defaultValue = "") query: String,
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "15") size: Int,
	) = service.searchProcessOccurrences(
		hostName = requireHostName(hostName),
		query = requireSearchQuery(query),
		page = requirePage(page),
		size = requirePageSize(size),
	).toOccurrencePageResponse()

	@GetMapping("/processes/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
	fun stream(@RequestParam hostName: String): SseEmitter =
		sseRegistry.subscribe(requireHostName(hostName))

	private fun requireHostName(hostName: String): String {
		if (hostName.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "hostName must not be blank")
		}
		return hostName
	}

	private fun requireSearchQuery(query: String): String {
		val trimmed = query.trim()
		if (trimmed.length > 128) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not exceed 128 characters")
		}
		return trimmed
	}

	private fun requirePage(page: Int): Int {
		if (page < 0) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be zero or greater")
		}
		return page
	}

	private fun requirePageSize(size: Int): Int {
		if (size !in 1..100) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100")
		}
		return size
	}
}
