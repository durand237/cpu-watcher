# Collector

## Purpose and data flow

`HostMetricsCollector` runs on a fixed-delay schedule. It retrieves OS processes and host CPU, RAM, and disk capacity through OSHI, then posts one timestamped JSON batch to the backend endpoint.

```text
OSHI process snapshot -> HostMetricsCollector -> POST /api/v1/metrics/processes -> backend -> PostgreSQL
```

Each batch contains the resolved host name, one collection timestamp, one host-level summary, and zero or more process metrics. The host name is chosen from `COMPUTERNAME`, then `HOSTNAME`, then the local DNS host name; it falls back to `unknown-host`.

## Metric calculation

- CPU percentage is calculated from OSHI's CPU load between the current and prior process snapshots, then divided by the logical CPU count and constrained to 0–100.
- A process with no prior snapshot reports 0 CPU percentage for its first sample.
- Memory bytes use the process's private resident memory. Memory percentage is that value divided by physical memory, constrained to 0–100.
- `COLLECTOR_MAX_PROCESSES=0` collects all visible processes; a positive value limits the OSHI query result.

Snapshots for processes that disappear are removed after every run, preventing the internal snapshot map from growing indefinitely.

Each batch also carries one host-level summary for the dashboard:

- CPU usage is OSHI's system CPU load between hardware CPU-tick snapshots.
- RAM usage is `(physical memory - available memory) / physical memory`.
- Disk usage is the used-space percentage across the file stores visible to the collector process.

The host summary and all process metrics use the same collection timestamp. Disk visibility follows the operating-system permissions and mounted filesystems available to the host-side collector.

## Resilience and security

Collection is skipped when `COLLECTOR_API_KEY` is blank, and the condition is logged. Any collection or HTTP error is caught, logged as a warning, and does not stop later scheduled attempts. The collector sends the API key in `X-Collector-Api-Key`; use a unique, non-empty secret and protect any network path to the backend with TLS.

## Running location

The collector should normally run directly on the host being observed. Its view of processes is constrained by operating-system permissions. A containerized collector typically sees only its container namespace, so it is unsuitable for host-wide monitoring without explicit host namespace and permission configuration. It is configured as a non-web Spring application, so it does not claim an HTTP port on the host.

The default collection interval is one second (`COLLECTOR_INTERVAL_MS=1000`), so connected dashboards receive a new SSE snapshot about once per second. When the supplied Compose deployment runs on the same machine, configure `COLLECTOR_BACKEND_URL=http://127.0.0.1:8080`. This route goes directly to the loopback-bound backend port; it does not use the browser-facing Nginx proxy. Set `COLLECTOR_API_KEY` to the same non-empty value in the root `.env` file.
