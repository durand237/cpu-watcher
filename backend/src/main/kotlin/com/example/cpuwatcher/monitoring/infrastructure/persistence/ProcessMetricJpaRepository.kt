package com.example.cpuwatcher.monitoring.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ProcessMetricJpaRepository : JpaRepository<ProcessMetricEntity, Long> {
	fun findByHostNameAndCollectedAtOrderByProcessIdAsc(hostName: String, collectedAt: java.time.Instant): List<ProcessMetricEntity>
}
