import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'

type ProcessMetric = {
  processId: number
  processName: string
  cpuUsagePercent: number
  memoryBytes: number
  memoryUsagePercent: number
}

type HostMetrics = {
  cpuUsagePercent: number
  memoryUsagePercent: number
  diskUsagePercent: number
}

type Snapshot = {
  hostName: string
  collectedAt: string
  hostMetrics?: HostMetrics
  processes: ProcessMetric[]
}

type HistoryPoint = HostMetrics & { collectedAt: string }

const apiBase = '/api/v1/metrics/processes'
const maxHistoryPoints = 60

function clamp(value: number) {
  return Math.min(100, Math.max(0, value))
}

function metricsFor(snapshot: Snapshot): HostMetrics {
  if (snapshot.hostMetrics) return snapshot.hostMetrics
  return {
    cpuUsagePercent: clamp(snapshot.processes.reduce((total, process) => total + process.cpuUsagePercent, 0)),
    memoryUsagePercent: clamp(snapshot.processes.reduce((total, process) => total + process.memoryUsagePercent, 0)),
    diskUsagePercent: 0,
  }
}

function formatPercent(value: number) {
  return `${value.toFixed(1)}%`
}

function formatBytes(bytes: number) {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB', 'TB']
  const unitIndex = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)) - 1, units.length - 1)
  return `${(bytes / 1024 ** (unitIndex + 1)).toFixed(1)} ${units[unitIndex]}`
}

function statusTone(percent: number) {
  if (percent >= 80) return 'busy'
  if (percent >= 60) return 'warning'
  return 'healthy'
}

function UsageRing({ label, percent }: { label: string; percent: number }) {
  const radius = 43
  const circumference = 2 * Math.PI * radius
  const safePercent = clamp(percent)

  return (
    <section className={`usage-card ${statusTone(safePercent)}`} aria-label={`${label}: ${formatPercent(safePercent)}`}>
      <div className="usage-ring" aria-hidden="true">
        <svg viewBox="0 0 112 112">
          <circle className="usage-ring__track" cx="56" cy="56" r={radius} />
          <circle className="usage-ring__value" cx="56" cy="56" r={radius} strokeDasharray={circumference} strokeDashoffset={circumference * (1 - safePercent / 100)} />
        </svg>
        <strong>{formatPercent(safePercent)}</strong>
      </div>
      <div><h2>{label}</h2><p>{safePercent >= 80 ? 'Very busy' : safePercent >= 60 ? 'Elevated usage' : 'Capacity available'}</p></div>
    </section>
  )
}

function UsageChart({ label, history, valueKey }: { label: string; history: HistoryPoint[]; valueKey: keyof HostMetrics }) {
  const points = history.map((point, index) => {
    const x = history.length === 1 ? 50 : (index / (history.length - 1)) * 100
    const y = 36 - (clamp(point[valueKey]) / 100) * 32
    return `${x},${y}`
  })
  const latest = history.length > 0 ? history[history.length - 1][valueKey] : 0

  return (
    <section className="chart-card">
      <div className="chart-card__heading"><h2>{label}</h2><strong>{formatPercent(latest)}</strong></div>
      <svg className="chart" viewBox="0 0 100 40" preserveAspectRatio="none" role="img" aria-label={`${label} over the last minute`}>
        <line x1="0" x2="100" y1="4" y2="4" /><line x1="0" x2="100" y1="20" y2="20" /><line x1="0" x2="100" y1="36" y2="36" />
        {points.length > 1 && <polyline points={points.join(' ')} />}
        {points.length === 1 && <circle cx="50" cy={points[0].split(',')[1]} r="1.8" />}
      </svg>
      <p>Last {Math.min(history.length, maxHistoryPoints)} samples</p>
    </section>
  )
}

function App() {
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null)
  const [history, setHistory] = useState<HistoryPoint[]>([])
  const [connection, setConnection] = useState<'connecting' | 'live' | 'error'>('connecting')
  const [error, setError] = useState<string | null>(null)

  const applySnapshot = useCallback((nextSnapshot: Snapshot) => {
    const metrics = metricsFor(nextSnapshot)
    setSnapshot(nextSnapshot)
    setHistory((current) => {
      if (current.length > 0 && current[current.length - 1].collectedAt === nextSnapshot.collectedAt) return current
      return [...current, { collectedAt: nextSnapshot.collectedAt, ...metrics }].slice(-maxHistoryPoints)
    })
  }, [])

  useEffect(() => {
    let active = true
    let retryTimer: ReturnType<typeof setTimeout> | undefined
    let eventSource: EventSource | undefined

    async function discoverHostAndConnect() {
      setConnection('connecting')
      try {
        const response = await fetch(`${apiBase}/latest`)
        if (response.status === 404) {
          if (active) {
            setError('Waiting for the host collector to send its first snapshot.')
            retryTimer = setTimeout(discoverHostAndConnect, 1000)
          }
          return
        }
        if (!response.ok) throw new Error(`Could not load metrics (${response.status}).`)

        const latestSnapshot = await response.json() as Snapshot
        if (!active) return
        applySnapshot(latestSnapshot)
        eventSource = new EventSource(`${apiBase}/stream?hostName=${encodeURIComponent(latestSnapshot.hostName)}`)
        eventSource.addEventListener('process-metric-snapshot', (event) => {
          applySnapshot(JSON.parse((event as MessageEvent<string>).data) as Snapshot)
          setConnection('live')
          setError(null)
        })
        eventSource.onopen = () => { setConnection('live'); setError(null) }
        eventSource.onerror = () => setConnection('error')
      } catch (loadError: unknown) {
        if (active) {
          setConnection('error')
          setError((loadError as Error).message)
          retryTimer = setTimeout(discoverHostAndConnect, 1000)
        }
      }
    }

    discoverHostAndConnect()
    return () => { active = false; if (retryTimer) clearTimeout(retryTimer); eventSource?.close() }
  }, [applySnapshot])

  const metrics = useMemo(() => snapshot ? metricsFor(snapshot) : { cpuUsagePercent: 0, memoryUsagePercent: 0, diskUsagePercent: 0 }, [snapshot])
  const processes = useMemo(() => snapshot?.processes.slice().sort((a, b) => b.cpuUsagePercent - a.cpuUsagePercent).slice(0, 15) ?? [], [snapshot])

  return (
    <main className="dashboard-shell">
      <div className="container-md">
        <header className="dashboard-header">
          <div><p className="eyebrow">CPU WATCHER</p><h1>System overview</h1><p className="subtitle">Live host capacity through the Nginx API proxy.</p></div>
          <div className={`connection connection--${connection}`}><span aria-hidden="true" />{connection === 'live' ? 'Live' : connection === 'connecting' ? 'Connecting' : 'Reconnecting'}</div>
        </header>

        <section className="host-picker" aria-label="Monitored host">
          <span>Monitored host</span><strong>{snapshot?.hostName ?? 'Waiting for the local collector'}</strong>
          {snapshot && <time dateTime={snapshot.collectedAt}>Updated {new Date(snapshot.collectedAt).toLocaleTimeString()}</time>}
        </section>
        {error && <p className="notice" role="status">{error}</p>}

        <section className="usage-grid" aria-label="Host resource usage"><UsageRing label="CPU" percent={metrics.cpuUsagePercent} /><UsageRing label="RAM" percent={metrics.memoryUsagePercent} /><UsageRing label="Disk" percent={metrics.diskUsagePercent} /></section>
        <section className="chart-grid" aria-label="Resource history"><UsageChart label="CPU trend" history={history} valueKey="cpuUsagePercent" /><UsageChart label="RAM trend" history={history} valueKey="memoryUsagePercent" /><UsageChart label="Disk trend" history={history} valueKey="diskUsagePercent" /></section>

        <section className="process-panel">
          <div className="process-panel__heading"><div><p className="eyebrow">ACTIVE PROCESSES</p><h2>Highest CPU usage</h2></div><span>{snapshot ? `${snapshot.processes.length} running` : 'Waiting for data'}</span></div>
          <div className="process-table-wrap"><table><thead><tr><th>Process</th><th>PID</th><th>CPU</th><th>RAM</th><th>Memory</th></tr></thead><tbody>
            {processes.map((process) => <tr key={`${process.processId}-${process.processName}`}><td>{process.processName}</td><td>{process.processId}</td><td>{formatPercent(process.cpuUsagePercent)}</td><td>{formatBytes(process.memoryBytes)}</td><td>{formatPercent(process.memoryUsagePercent)}</td></tr>)}
            {processes.length === 0 && <tr><td colSpan={5} className="empty-state">No process data available.</td></tr>}
          </tbody></table></div>
        </section>
        <footer><span className="legend healthy">Green: capacity available</span><span className="legend warning">Amber: elevated</span><span className="legend busy">Red: very busy</span></footer>
      </div>
    </main>
  )
}

export default App
