package com.example.cpuwatcher.monitoring.infrastructure.persistence

import com.example.cpuwatcher.monitoring.domain.HostMetrics
import com.example.cpuwatcher.monitoring.domain.ProcessMetricBatch
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
	name = "host_metric_snapshots",
	indexes = [Index(name = "idx_host_metric_snapshots_host_collected_at", columnList = "host_name,collected_at")],
)
class HostMetricSnapshotEntity(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,
	@Column(name = "host_name", nullable = false, length = 255)
	var hostName: String = "",
	@Column(name = "collected_at", nullable = false)
	var collectedAt: Instant = Instant.EPOCH,
	@Column(name = "cpu_usage_percent", nullable = false)
	var cpuUsagePercent: Double = 0.0,
	@Column(name = "memory_usage_percent", nullable = false)
	var memoryUsagePercent: Double = 0.0,
	@Column(name = "disk_usage_percent", nullable = false)
	var diskUsagePercent: Double = 0.0,
)

fun ProcessMetricBatch.toSnapshotEntity(): HostMetricSnapshotEntity = HostMetricSnapshotEntity(
	hostName = hostName,
	collectedAt = collectedAt,
	cpuUsagePercent = hostMetrics.cpuUsagePercent,
	memoryUsagePercent = hostMetrics.memoryUsagePercent,
	diskUsagePercent = hostMetrics.diskUsagePercent,
)

fun HostMetricSnapshotEntity.toHostMetrics(): HostMetrics = HostMetrics(
	cpuUsagePercent = cpuUsagePercent,
	memoryUsagePercent = memoryUsagePercent,
	diskUsagePercent = diskUsagePercent,
)
