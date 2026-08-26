# Backend

## Purpose

The backend accepts one timestamped host-metric batch at `POST /api/v1/metrics/processes` and persists its process rows and host-level summary. It returns HTTP `202 Accepted` and `{ "accepted": <count> }` after the batch is saved. It also exposes the latest stored batch for a host and an SSE stream of newly committed batches.

## Request contract

```json
{
  "hostName": "workstation-01",
  "collectedAt": "2026-08-25T12:00:00Z",
  "processes": [
    {
      "processId": 1234,
      "processName": "java",
      "cpuUsagePercent": 12.5,
      "memoryBytes": 524288000,
      "memoryUsagePercent": 3.1
    }
  ],
  "hostMetrics": {
    "cpuUsagePercent": 24.7,
    "memoryUsagePercent": 68.2,
    "diskUsagePercent": 41.5
  }
}
```

The host name must be non-blank and at most 255 characters; process names must be non-blank and at most 512 characters. Process IDs and memory bytes cannot be negative, and every percentage value must be between 0 and 100. `hostMetrics` describes the whole observed host at the same `collectedAt` timestamp as the process rows. Invalid requests receive Spring MVC validation errors.

## Authentication

`CollectorApiKeyInterceptor` protects the collector-ingestion route, `POST /api/v1/metrics/processes`. A request must include `X-Collector-Api-Key` that equals the non-empty `COLLECTOR_API_KEY` value. The comparison uses `MessageDigest.isEqual` to avoid an early-exit string comparison. Missing or invalid keys receive HTTP `401`.

The browser read endpoints are intentionally unauthenticated for the local development deployment. They must receive an application authentication mechanism before Nginx is exposed beyond loopback.

Actuator's `health` and `info` endpoints are exposed without this collector-key interceptor.

## Read and live-stream API

`GET /api/v1/metrics/processes/latest?hostName=<host>` returns the most recent complete persisted batch for that host, including `hostMetrics` (`cpuUsagePercent`, `memoryUsagePercent`, and `diskUsagePercent`). Omit `hostName` to receive the newest batch from any collector host; this is the dashboard's automatic host-discovery route. It returns `404 Not Found` if no matching host has stored metrics.

`GET /api/v1/metrics/processes/stream?hostName=<host>` returns `text/event-stream`. After a collector batch commits successfully, the backend sends that complete batch to open streams for the matching host as an SSE event named `process-metric-snapshot`. A frontend should fetch `/latest` on initial load, then open the stream for future updates.

The live stream is in-memory and is suitable for the current single backend container. It does not replay missed events; reconnecting clients should fetch `/latest` again. A multi-instance deployment needs a shared event broker before stream events can reach clients connected to different backend instances.

## Persistence

The application uses Spring Data JPA with PostgreSQL. `ProcessMetricRepositoryAdapter` saves each batch transactionally: process rows go to `process_metrics` and the one host-level summary goes to `host_metric_snapshots`. Flyway creates the process table in `V1__create_process_metrics.sql` and the snapshot table in `V2__create_host_metric_snapshots.sql`. The latest endpoint first finds the latest host snapshot and then returns its matching process rows.

The current Compose deployment supplies the database connection through `SPRING_DATASOURCE_*` environment variables and waits for PostgreSQL's health check before starting the API. Flyway runs before JPA validates the mapping (`spring.jpa.hibernate.ddl-auto=validate`), so a new database is initialized automatically. `spring.flyway.baseline-on-migrate=true` also baselines an existing pre-Flyway process table at V1, preserving its rows while applying later migrations such as V2.

## Deployment boundary

Compose divides services across four networks:

- `cpu-watcher-public` contains Nginx and the frontend development container. Nginx binds to `127.0.0.1:80` as the production browser entry point; production frontend assets can be mounted there when deployed. During development, the frontend's Vite proxy forwards its `/api/` requests to Nginx, which in turn proxies them to the backend over the application-private network.
- `cpu-watcher-host-access` contains the backend only. It is a dedicated bridge that lets Docker Desktop publish the backend's loopback-only `127.0.0.1:8080` port for the host-side collector; neither Nginx nor the frontend joins it.
- `cpu-watcher-private` contains Nginx and the backend only. It is an internal Docker network; Nginx uses the service name `cpu-watcher:8080` to reach the API.
- `cpu-watcher-db-private` contains the backend, PostgreSQL, and the retention worker only. It is internal and PostgreSQL has no published host port.

The backend also binds directly to `127.0.0.1:8080` for the host collector. This keeps ingestion available to a collector running on the same machine without exposing the API to remote clients. The collector does not traverse Nginx.

Nginx proxies the API routes that exist. The frontend fetches the latest stored snapshot and subscribes to the SSE route through this proxy, while collector ingestion remains authenticated. Before exposing Nginx beyond loopback, add browser authentication, TLS, and an ingress/firewall policy.
Nginx disables response buffering specifically for the SSE route so a committed batch is forwarded to the browser immediately.
