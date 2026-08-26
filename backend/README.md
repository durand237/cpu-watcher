# CPU Watcher backend

The backend receives process metrics from a collector and persists each sample in PostgreSQL.

## Requirements

- Java 17
- PostgreSQL 16 when running outside Docker
- `COLLECTOR_API_KEY` set to the same non-empty value used by the collector

## Run locally

Set the datasource properties and collector key, then start the application:

```powershell
$env:COLLECTOR_API_KEY = "local-development-key"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/cpu_watcher"
$env:SPRING_DATASOURCE_USERNAME = "cpu_watcher"
$env:SPRING_DATASOURCE_PASSWORD = "your-password"
.\gradlew.bat bootRun
```

`POST /api/v1/metrics/processes` accepts a batch from a collector and returns `202 Accepted` with the number of accepted records. The endpoint requires the `X-Collector-Api-Key` header and validates host name, process name, non-negative values, and percentage ranges.

The health endpoint is available at `GET /actuator/health`.

## Verification

```powershell
.\gradlew.bat test
```

The test profile uses an in-memory H2 database, so no local PostgreSQL instance is needed for this check.
