package com.example.cpuwatcher.monitoring.api

import com.example.cpuwatcher.monitoring.api.dto.ProcessMetricBatchRequest
import com.example.cpuwatcher.monitoring.api.dto.ProcessMetricBatchResponse
import com.example.cpuwatcher.monitoring.api.dto.ProcessMetricSnapshotResponse
import com.example.cpuwatcher.monitoring.api.dto.toSnapshotResponse
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

	@GetMapping("/processes/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
	fun stream(@RequestParam hostName: String): SseEmitter =
		sseRegistry.subscribe(requireHostName(hostName))

	private fun requireHostName(hostName: String): String {
		if (hostName.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "hostName must not be blank")
		}
		return hostName
	}
}
