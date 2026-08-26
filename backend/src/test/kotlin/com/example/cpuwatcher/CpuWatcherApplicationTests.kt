package com.example.cpuwatcher

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(properties = ["collector.api-key=collector-test-key"])
@AutoConfigureMockMvc
class CpuWatcherApplicationTests {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Test
	fun contextLoads() {
	}

	@Test
	fun `returns the latest complete snapshot for a host`() {
		mockMvc.perform(
			post("/api/v1/metrics/processes")
				.header("X-Collector-Api-Key", "collector-test-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "hostName": "workstation-01",
					  "collectedAt": "2026-08-25T12:00:00Z",
					  "processes": [{
					    "processId": 1234,
					    "processName": "java",
					    "cpuUsagePercent": 12.5,
					    "memoryBytes": 524288000,
					    "memoryUsagePercent": 3.1
					  }],
					  "hostMetrics": {
					    "cpuUsagePercent": 24.7,
					    "memoryUsagePercent": 68.2,
					    "diskUsagePercent": 41.5
					  }
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isAccepted)

		mockMvc.perform(get("/api/v1/metrics/processes/latest").param("hostName", "workstation-01"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.hostName").value("workstation-01"))
			.andExpect(jsonPath("$.hostMetrics.diskUsagePercent").value(41.5))
			.andExpect(jsonPath("$.processes[0].processName").value("java"))

		mockMvc.perform(get("/api/v1/metrics/processes/latest"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.hostName").value("workstation-01"))

		mockMvc.perform(
			get("/api/v1/metrics/processes/search")
				.param("hostName", "workstation-01")
				.param("query", "jav"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.occurrences[0].processName").value("java"))
			.andExpect(jsonPath("$.occurrences[0].collectedAt").value("2026-08-25T12:00:00Z"))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.totalPages").value(1))
	}
}
