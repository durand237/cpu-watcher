package com.example.cpuwatcher.monitoring.application

import com.example.cpuwatcher.monitoring.domain.ProcessMetricBatch
import com.example.cpuwatcher.monitoring.domain.ProcessMetric

interface ProcessMetricRepository {
	fun save(batch: ProcessMetricBatch): Int
	fun findLatestSnapshot(hostName: String? = null): ProcessMetricBatch?
	fun searchProcessOccurrences(hostName: String, query: String, page: Int, size: Int): ProcessMetricSearchPage
}

data class ProcessMetricSearchPage(
	val occurrences: List<ProcessMetric>,
	val page: Int,
	val size: Int,
	val totalElements: Long,
	val totalPages: Int,
)
