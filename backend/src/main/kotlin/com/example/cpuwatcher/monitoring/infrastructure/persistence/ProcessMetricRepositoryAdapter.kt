package com.example.cpuwatcher.monitoring.infrastructure.persistence

import com.example.cpuwatcher.monitoring.application.ProcessMetricRepository
import com.example.cpuwatcher.monitoring.domain.ProcessMetric
import com.example.cpuwatcher.monitoring.domain.ProcessMetricBatch
import org.springframework.stereotype.Repository

@Repository
class ProcessMetricRepositoryAdapter(
	private val jpaRepository: ProcessMetricJpaRepository,
	private val snapshotJpaRepository: HostMetricSnapshotJpaRepository,
) : ProcessMetricRepository {

	override fun save(batch: ProcessMetricBatch): Int {
		snapshotJpaRepository.save(batch.toSnapshotEntity())
		jpaRepository.saveAll(batch.processes.map(ProcessMetric::toEntity))
		return batch.processes.size
	}

	override fun findLatestSnapshot(hostName: String?): ProcessMetricBatch? {
		val snapshot = if (hostName == null) {
			snapshotJpaRepository.findFirstByOrderByCollectedAtDesc()
		} else {
			snapshotJpaRepository.findFirstByHostNameOrderByCollectedAtDesc(hostName!!)
		} ?: return null
		val metrics = jpaRepository.findByHostNameAndCollectedAtOrderByProcessIdAsc(snapshot.hostName, snapshot.collectedAt)
			.map(ProcessMetricEntity::toDomain)
		return ProcessMetricBatch(
			hostName = snapshot.hostName,
			collectedAt = snapshot.collectedAt,
			hostMetrics = snapshot.toHostMetrics(),
			processes = metrics,
		)
	}
}
