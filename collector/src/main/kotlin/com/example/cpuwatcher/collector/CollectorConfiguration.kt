package com.example.cpuwatcher.collector

import oshi.SystemInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class CollectorConfiguration {

	@Bean
	fun systemInfo(): SystemInfo = SystemInfo()

	@Bean
	fun restClient(
		@Value("\${collector.backend-url}") backendUrl: String,
	): RestClient = RestClient.builder().baseUrl(backendUrl).build()
}
