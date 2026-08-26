package com.example.cpuwatcher

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CpuWatcherApplication

fun main(args: Array<String>) {
	runApplication<CpuWatcherApplication>(*args)
}
