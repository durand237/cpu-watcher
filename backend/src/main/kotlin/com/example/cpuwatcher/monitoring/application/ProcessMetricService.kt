package com.example.cpuwatcher.monitoring.application

import com.example.cpuwatcher.monitoring.domain.ProcessMetricBatch

interface ProcessMetricService {
	fun ingest(batch: ProcessMetricBatch): Int

	fun findLatestSnapshot(hostName: String? = null): ProcessMetricBatch?
}

data class ProcessMetricBatchStoredEvent(
	val batch: ProcessMetricBatch,
)
