package com.example.cpuwatcher.monitoring.application

import com.example.cpuwatcher.monitoring.domain.ProcessMetricBatch
import com.example.cpuwatcher.monitoring.domain.ProcessMetric

interface ProcessMetricService {
	fun ingest(batch: ProcessMetricBatch): Int

	fun findLatestSnapshot(hostName: String? = null): ProcessMetricBatch?

	fun searchProcessOccurrences(hostName: String, query: String, page: Int, size: Int): ProcessMetricSearchPage
}

data class ProcessMetricBatchStoredEvent(
	val batch: ProcessMetricBatch,
)
