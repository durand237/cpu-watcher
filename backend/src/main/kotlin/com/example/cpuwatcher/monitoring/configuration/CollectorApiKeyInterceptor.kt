package com.example.cpuwatcher.monitoring.configuration

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Component
class CollectorApiKeyInterceptor(
	@Value("\${collector.api-key:}")
	private val configuredApiKey: String,
) : HandlerInterceptor {

	override fun preHandle(
		request: HttpServletRequest,
		response: HttpServletResponse,
		handler: Any,
	): Boolean {
		val suppliedApiKey = request.getHeader("X-Collector-Api-Key")
		val valid = configuredApiKey.isNotBlank() && suppliedApiKey != null &&
			MessageDigest.isEqual(
				configuredApiKey.toByteArray(StandardCharsets.UTF_8),
				suppliedApiKey.toByteArray(StandardCharsets.UTF_8),
			)

		if (!valid) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid collector API key")
			return false
		}

		return true
	}
}
