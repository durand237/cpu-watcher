package com.example.cpuwatcher.monitoring.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfiguration(
	private val collectorApiKeyInterceptor: CollectorApiKeyInterceptor,
) : WebMvcConfigurer {

	override fun addInterceptors(registry: InterceptorRegistry) {
		registry.addInterceptor(collectorApiKeyInterceptor)
			.addPathPatterns("/api/v1/metrics/processes")
	}
}
