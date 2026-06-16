import { useEffect, useState } from 'react'
import { apiClient } from '../../api/client.js'
import { Spinner } from './Spinner.jsx'

const STATUS = {
  LOADING: 'loading',
  UP: 'up',
  DOWN: 'down',
}

const variant = {
  up: {
    dot: 'bg-forest',
    text: 'text-forest',
    label: 'Online',
    ring: 'bg-forest/10',
  },
  down: {
    dot: 'bg-dried-red',
    text: 'text-dried-red',
    label: 'Sin conexión',
    ring: 'bg-dried-red/10',
  },
  loading: {
    dot: 'bg-muted',
    text: 'text-muted',
    label: 'Comprobando…',
    ring: 'bg-muted/10',
  },
}

function checkHealth({ onUp, onDown }) {
  const start = performance.now()
    apiClient
    .get('/api/health')
    .then(({ data }) => {
      if (data?.status === 'UP') {
        onUp(Math.round(performance.now() - start))
      } else {
        onDown()
      }
    })
    .catch(() => onDown())
}

export function HealthBadge() {
  const [status, setStatus] = useState(STATUS.LOADING)
  const [latency, setLatency] = useState(null)

  useEffect(() => {
    checkHealth({
      onUp: (ms) => {
        setStatus(STATUS.UP)
        setLatency(ms)
      },
      onDown: () => setStatus(STATUS.DOWN),
    })
  }, [])

  const handleClick = () => {
    setStatus(STATUS.LOADING)
    checkHealth({
      onUp: (ms) => {
        setStatus(STATUS.UP)
        setLatency(ms)
      },
      onDown: () => setStatus(STATUS.DOWN),
    })
  }

  const v = variant[status]

  return (
    <button
      type="button"
      onClick={handleClick}
      data-focus-ring
      aria-live="polite"
      aria-label={`Estado del backend: ${v.label}. Click para reintentar.`}
      className={`group inline-flex items-center gap-2.5 rounded-full border border-border ${v.ring} px-3.5 py-1.5 text-xs font-medium transition-all hover:border-ink/30`}
    >
      <span className="relative flex h-2 w-2">
        {status === STATUS.UP && (
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-forest opacity-60" />
        )}
        <span className={`relative inline-flex h-2 w-2 rounded-full ${v.dot}`} />
      </span>
      <span className={v.text}>
        {status === STATUS.LOADING ? (
          <span className="inline-flex items-center gap-1.5">
            <Spinner size={11} /> {v.label}
          </span>
        ) : (
          v.label
        )}
      </span>
      {latency != null && status === STATUS.UP && (
        <span className="font-mono text-[10px] text-muted">
          {latency}ms
        </span>
      )}
    </button>
  )
}
