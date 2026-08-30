package com.example.cpuwatcher.collector

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.runApplication
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CollectorApplication {
	@Bean
	fun versionLogger(buildProperties: ObjectProvider<BuildProperties>) = ApplicationRunner {
		logger.info("CPU Watcher collector version {}", buildProperties.ifAvailable?.version ?: "development")
	}

	private companion object {
		val logger = LoggerFactory.getLogger(CollectorApplication::class.java)
	}
}

fun main(args: Array<String>) {
	runApplication<CollectorApplication>(*args)
}
