package com.example.cpuwatcher.monitoring.application

import com.example.cpuwatcher.monitoring.domain.ProcessMetricBatch
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProcessMetricServiceImpl(
	private val repository: ProcessMetricRepository,
	private val eventPublisher: ApplicationEventPublisher,
) : ProcessMetricService {

	@Transactional
	override fun ingest(batch: ProcessMetricBatch): Int {
		val accepted = repository.save(batch)
		eventPublisher.publishEvent(ProcessMetricBatchStoredEvent(batch))
		return accepted
	}

	@Transactional(readOnly = true)
	override fun findLatestSnapshot(hostName: String?): ProcessMetricBatch? =
		repository.findLatestSnapshot(hostName)

	@Transactional(readOnly = true)
	override fun searchProcessOccurrences(hostName: String, query: String, page: Int, size: Int) =
		repository.searchProcessOccurrences(hostName, query, page, size)
}
