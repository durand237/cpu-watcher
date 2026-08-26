# CPU Watcher collector

The collector is a host-side Spring Boot service. At a fixed interval it uses OSHI to read processes and sends a batch to the backend.

Run it on the host you want to observe. Running it in a container reports the container's visible processes instead of the host's unless the container is deliberately given host access.

## Requirements

- Java 17
- A running CPU Watcher backend
- A non-empty `COLLECTOR_API_KEY` matching the backend configuration

## Configuration

Configuration is supplied as Spring properties or environment variables.

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `COLLECTOR_BACKEND_URL` | `http://127.0.0.1:8080` | Backend base URL |
| `COLLECTOR_API_KEY` | none | Required shared API key |
| `COLLECTOR_INTERVAL_MS` | `5000` | Delay between completed collection attempts |
| `COLLECTOR_INITIAL_DELAY_MS` | `5000` | Delay before the first attempt |
| `COLLECTOR_MAX_PROCESSES` | `0` | Maximum processes sent; `0` sends all processes |

## Run

```powershell
$env:COLLECTOR_BACKEND_URL = "http://127.0.0.1:8080"
$env:COLLECTOR_API_KEY = "local-development-key"
.\gradlew.bat bootRun
```

The first observation of a process has `cpuUsagePercent` set to `0`: OSHI needs a previous process snapshot to calculate CPU use between ticks. Subsequent samples include the calculated CPU percentage. Failures to reach the backend are logged and the next scheduled attempt continues.

## Verification

```powershell
.\gradlew.bat test
```
