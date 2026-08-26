package com.example.cpuwatcher.monitoring.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProcessMetricJpaRepository : JpaRepository<ProcessMetricEntity, Long> {
	fun findByHostNameAndCollectedAtOrderByProcessIdAsc(hostName: String, collectedAt: java.time.Instant): List<ProcessMetricEntity>

	@Query(
		value = """
		SELECT * FROM process_metrics
		WHERE host_name = :hostName
		  AND (
			:query = ''
			OR
			process_name ILIKE CONCAT('%', :query, '%')
			OR CAST(process_id AS VARCHAR) LIKE CONCAT('%', :query, '%')
		  )
		ORDER BY collected_at DESC, process_id ASC
		""",
		countQuery = """
		SELECT COUNT(*) FROM process_metrics
		WHERE host_name = :hostName
		  AND (
			:query = ''
			OR process_name ILIKE CONCAT('%', :query, '%')
			OR CAST(process_id AS VARCHAR) LIKE CONCAT('%', :query, '%')
		  )
		""",
		nativeQuery = true,
	)
	fun searchByHostName(
		@Param("hostName") hostName: String,
		@Param("query") query: String,
		pageable: Pageable,
	): Page<ProcessMetricEntity>
}
