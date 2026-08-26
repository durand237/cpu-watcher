package com.example.cpuwatcher.monitoring.application

import com.example.cpuwatcher.monitoring.domain.ProcessMetricBatch

interface ProcessMetricRepository {
	fun save(batch: ProcessMetricBatch): Int
	fun findLatestSnapshot(hostName: String? = null): ProcessMetricBatch?
}
