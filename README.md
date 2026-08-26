# CPU Watcher

CPU Watcher stores per-process CPU and memory samples reported by a lightweight host collector. This repository currently contains two Kotlin/Spring Boot projects and a Docker Compose deployment for the backend database and REST API.

## Components

| Component | Location | Responsibility |
| --- | --- | --- |
| Backend API | [`backend/`](backend/README.md) | Authenticates metric batches, validates them, and stores them in PostgreSQL |
| Host collector | [`collector/`](collector/README.md) | Uses OSHI to collect process metrics and posts them to the backend |
| Local deployment | [`compose.yaml`](compose.yaml) | Starts Nginx, the backend API, PostgreSQL, and retention cleanup |

## Quick start

1. Copy `.env.example` to `.env` and set strong values for `POSTGRES_PASSWORD` and `COLLECTOR_API_KEY`.
2. Start PostgreSQL and the backend:

   ```powershell
   docker compose up --build -d
   ```

3. Run the collector on the host to monitor, using the same API key:

   ```powershell
   $env:COLLECTOR_BACKEND_URL = "http://127.0.0.1:8080"
   $env:COLLECTOR_API_KEY = "replace-with-your-collector-key"
   Set-Location collector
   .\gradlew.bat bootRun
   ```

4. Confirm API readiness at [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health). Nginx is available at [http://localhost](http://localhost).

5. Read the latest stored snapshot for a collector host, replacing `MyLaptop` with the reported host name:

   ```powershell
   Invoke-RestMethod "http://localhost/api/v1/metrics/processes/latest?hostName=MyLaptop"
   ```

   A browser frontend can open the live stream at `http://localhost/api/v1/metrics/processes/stream?hostName=MyLaptop` with `EventSource`.

6. Start all Docker services and the host collector in the background.

   **Windows PowerShell:**

   ```powershell
   docker compose up -d --build

   $line = Get-Content .env | Where-Object { $_ -match '^COLLECTOR_API_KEY=' } | Select-Object -First 1
   $env:COLLECTOR_API_KEY = $line.Substring('COLLECTOR_API_KEY='.Length)
   $env:COLLECTOR_BACKEND_URL = 'http://127.0.0.1:8080'
   Start-Process -FilePath "$PWD\collector\gradlew.bat" -ArgumentList 'bootRun' -WorkingDirectory "$PWD\collector" -WindowStyle Hidden
   ```

   **Linux/Ubuntu:**

   ```bash
   docker compose up -d --build

   export COLLECTOR_API_KEY="$(sed -n 's/^COLLECTOR_API_KEY=//p' .env)"
   export COLLECTOR_BACKEND_URL='http://127.0.0.1:8080'
   (cd collector && nohup ./gradlew bootRun > ../collector.log 2>&1 & echo $! > ../.collector.pid)
   ```

7. Stop the host collector and all Docker services.

   **Windows PowerShell:**

   ```powershell
   Get-CimInstance Win32_Process | Where-Object { $PSItem.CommandLine -match 'CollectorApplicationKt' } | ForEach-Object { Stop-Process -Id $PSItem.ProcessId -Force }; docker compose down
   ```

   **Linux/Ubuntu:**

   ```bash
   if [ -f .collector.pid ]; then kill "$(cat .collector.pid)" 2>/dev/null || true; rm .collector.pid; fi
   pkill -f 'CollectorApplicationKt' 2>/dev/null || true
   docker compose down
   ```

The collector API is published only on `127.0.0.1:8080`; it is intentionally not exposed to the network by the supplied Compose file. Nginx listens on `127.0.0.1:80` and proxies `/api/` to the backend over a private Docker network. PostgreSQL has no published port. For remote access, add TLS and an explicit ingress/firewall policy instead of publishing the backend directly.

## Development

Both Gradle projects use Java 17. Run checks independently:

```powershell
Set-Location backend; .\gradlew.bat test
Set-Location ..\collector; .\gradlew.bat test
```

Architecture and operational notes are in [`wiki/`](wiki/README.md).
