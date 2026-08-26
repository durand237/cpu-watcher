# Frontend

## Purpose

The React/Vite frontend is a responsive live dashboard for one monitored host. It presents current CPU, RAM, and disk usage as colour-coded circular diagrams, keeps a one-minute history chart for each metric, and lists the processes using the most CPU.

Green means capacity is available (under 60%), amber means elevated use (60% through 79.9%), and red means very busy (80% or above). The layout uses a `container-md`-sized wrapper that contracts to a single column on smaller screens.

## API data flow

```text
Browser -> Vite :5173 -> /api proxy -> nginx -> cpu-watcher -> PostgreSQL
                                         ^
                                         | SSE process-metric-snapshot events
```

The browser requests only relative `/api/v1/metrics/processes/...` URLs. In the frontend container, Vite forwards `/api` to the `nginx` service over `cpu-watcher-public`; Nginx then reaches the API through `cpu-watcher-private`. This preserves the browser-facing proxy boundary and never attaches the frontend to the backend-private or database-private networks.

On load, the dashboard requests `GET /api/v1/metrics/processes/latest` without a host name. The backend returns the most recent collector snapshot, including the collector-provided machine name. The dashboard then opens `GET /api/v1/metrics/processes/stream?hostName=<host>` for that discovered host and updates as each named `process-metric-snapshot` SSE event arrives. The collector's default one-second interval supplies the refresh cadence. A 404 means the host collector has not sent its first batch yet; the dashboard retries every second.

The snapshot carries:

- `hostMetrics.cpuUsagePercent`, `memoryUsagePercent`, and `diskUsagePercent` for the three status rings and trend charts.
- `processes`, sorted in the UI by CPU percentage for the running-process table.

## Run

Start the complete local deployment:

```powershell
docker compose up --build -d
```

Open [http://localhost:5173](http://localhost:5173). The **Monitored host** value is filled automatically from the collector's submitted machine name; no browser-side host-name configuration is required.

For the dashboard to show data, run the host-side collector with the same `COLLECTOR_API_KEY` as the backend. The backend API routes remain loopback-only externally; the development dashboard reaches them through Nginx's internal proxy path.

## Development and verification

The `frontend` Compose service is attached to `cpu-watcher-public`, publishes only `127.0.0.1:5173`, and keeps its `node_modules` in the `frontend-node-modules` Docker volume. It runs `npm ci` before starting Vite.

Run the production type-check and bundle build inside that service:

```powershell
docker compose exec -T frontend npm run build
```

The frontend deliberately uses native SVG and CSS for its rings and charts, so it has no chart-library dependency.
