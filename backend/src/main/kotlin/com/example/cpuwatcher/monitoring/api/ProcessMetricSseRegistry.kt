package com.example.cpuwatcher.monitoring.api

import com.example.cpuwatcher.monitoring.api.dto.toSnapshotResponse
import com.example.cpuwatcher.monitoring.application.ProcessMetricBatchStoredEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class ProcessMetricSseRegistry {
	private val emittersByHost = ConcurrentHashMap<String, MutableSet<SseEmitter>>()

	fun subscribe(hostName: String): SseEmitter {
		val emitter = SseEmitter(0L)
		val hostEmitters = emittersByHost.computeIfAbsent(hostName) { ConcurrentHashMap.newKeySet() }
		hostEmitters.add(emitter)

		val removeEmitter = { remove(hostName, emitter) }
		emitter.onCompletion(removeEmitter)
		emitter.onTimeout(removeEmitter)
		emitter.onError { removeEmitter() }
		return emitter
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	fun publishStoredBatch(event: ProcessMetricBatchStoredEvent) {
		val batch = event.batch
		emittersByHost[batch.hostName]?.toList()?.forEach { emitter ->
			try {
				emitter.send(
					SseEmitter.event()
						.name("process-metric-snapshot")
						.data(batch.toSnapshotResponse()),
				)
			} catch (_: Exception) {
				remove(batch.hostName, emitter)
				emitter.complete()
			}
		}
	}

	private fun remove(hostName: String, emitter: SseEmitter) {
		emittersByHost[hostName]?.let { emitters ->
			emitters.remove(emitter)
			if (emitters.isEmpty()) {
				emittersByHost.remove(hostName, emitters)
			}
		}
	}
}
