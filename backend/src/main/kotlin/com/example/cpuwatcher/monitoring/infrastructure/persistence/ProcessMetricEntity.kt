package com.example.cpuwatcher.monitoring.infrastructure.persistence

import com.example.cpuwatcher.monitoring.domain.ProcessMetric
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
	name = "process_metrics",
	indexes = [
		Index(name = "idx_process_metrics_host_collected_at", columnList = "host_name,collected_at"),
		Index(name = "idx_process_metrics_process_collected_at", columnList = "process_id,collected_at"),
	],
)
class ProcessMetricEntity(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,
	@Column(name = "host_name", nullable = false, length = 255)
	var hostName: String = "",
	@Column(name = "collected_at", nullable = false)
	var collectedAt: Instant = Instant.EPOCH,
	@Column(name = "process_id", nullable = false)
	var processId: Long = 0,
	@Column(name = "process_name", nullable = false, length = 512)
	var processName: String = "",
	@Column(name = "cpu_usage_percent", nullable = false)
	var cpuUsagePercent: Double = 0.0,
	@Column(name = "memory_bytes", nullable = false)
	var memoryBytes: Long = 0,
	@Column(name = "memory_usage_percent", nullable = false)
	var memoryUsagePercent: Double = 0.0,
)

fun ProcessMetric.toEntity(): ProcessMetricEntity = ProcessMetricEntity(
	hostName = hostName,
	collectedAt = collectedAt,
	processId = processId,
	processName = processName,
	cpuUsagePercent = cpuUsagePercent,
	memoryBytes = memoryBytes,
	memoryUsagePercent = memoryUsagePercent,
)

fun ProcessMetricEntity.toDomain(): ProcessMetric = ProcessMetric(
	hostName = hostName,
	collectedAt = collectedAt,
	processId = processId,
	processName = processName,
	cpuUsagePercent = cpuUsagePercent,
	memoryBytes = memoryBytes,
	memoryUsagePercent = memoryUsagePercent,
)
