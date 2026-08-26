package com.example.cpuwatcher.collector

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CollectorApplication

fun main(args: Array<String>) {
	runApplication<CollectorApplication>(*args)
}
