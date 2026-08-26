package com.example.cpuwatcher.monitoring.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface HostMetricSnapshotJpaRepository : JpaRepository<HostMetricSnapshotEntity, Long> {
	fun findFirstByHostNameOrderByCollectedAtDesc(hostName: String): HostMetricSnapshotEntity?
	fun findFirstByOrderByCollectedAtDesc(): HostMetricSnapshotEntity?
}
