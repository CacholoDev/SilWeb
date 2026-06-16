import { useEffect, useState } from 'react'
import { ArrowRight, Leaf, Sparkles } from 'lucide-react'
import { apiClient } from '../api/client.js'
import { Spinner } from '../components/common/Spinner.jsx'

const LOADING_STATE = { status: 'loading', latency: null, detail: null }

function fetchHealthSnapshot() {
  const start = performance.now()
    return apiClient
    .get('/api/health')
    .then(({ data }) => ({
      status: data?.status === 'UP' ? 'up' : 'down',
      latency: Math.round(performance.now() - start),
      detail: data?.status === 'UP' ? 'Backend responde correctamente' : 'Estado inesperado',
    }))
    .catch((err) => ({
      status: 'down',
      latency: null,
      detail: err?.detail ?? 'Sin respuesta',
    }))
}

function StatusCard() {
  const [state, setState] = useState(LOADING_STATE)

  useEffect(() => {
    fetchHealthSnapshot().then(setState)
  }, [])

  const handleClick = () => {
    setState(LOADING_STATE)
    fetchHealthSnapshot().then(setState)
  }

  const palette = {
    up: 'border-forest/30 bg-forest/[0.04] text-forest',
    down: 'border-dried-red/30 bg-dried-red/[0.04] text-dried-red',
    loading: 'border-border bg-parchment-deep text-muted',
  }

  return (
    <div className="rounded-2xl border border-border bg-white/40 p-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-muted">
            Backend
          </p>
          <p className="mt-1 font-display text-xl text-ink">
            {state.status === 'loading' ? (
              <span className="inline-flex items-center gap-2">
                <Spinner size={16} /> Comprobando…
              </span>
            ) : state.status === 'up' ? (
              'Conectado'
            ) : (
              'Sin conexión'
            )}
          </p>
        </div>
        <button
          type="button"
          onClick={handleClick}
          data-focus-ring
          className="rounded-full border border-border px-4 py-1.5 text-xs font-medium text-ink-soft transition-colors hover:border-ink hover:text-ink"
        >
          Reintentar
        </button>
      </div>
      <div
        className={`mt-4 rounded-lg border px-4 py-3 text-sm ${palette[state.status]}`}
      >
        <p>{state.detail ?? '—'}</p>
        {state.latency != null && (
          <p className="mt-1 font-mono text-[11px] opacity-80">
            GET /api/health · {state.latency} ms
          </p>
        )}
      </div>
    </div>
  )
}

function Pillar({ icon: Icon, title, body }) {
  return (
    <article className="border-t border-border pt-6">
      <Icon size={20} className="text-oxblood" strokeWidth={1.5} />
      <h3 className="mt-3 font-display text-lg text-ink">{title}</h3>
      <p className="mt-1.5 text-sm leading-relaxed text-ink-soft">{body}</p>
    </article>
  )
}

export function Landing() {
  return (
    <>
      <section className="mx-auto max-w-6xl px-6 pt-16 pb-20 md:px-10 md:pt-24 md:pb-28">
        <p className="font-mono text-[11px] uppercase tracking-[0.28em] text-oxblood">
          Edición limitada · Otoño 2026
        </p>
        <h1 className="mt-6 max-w-3xl font-display text-5xl leading-[1.05] text-balance text-ink md:text-7xl">
          Cosas de casa,{' '}
          <em className="font-light italic text-oxblood">hechas con oficio.</em>
        </h1>
        <p className="mt-7 max-w-xl text-lg leading-relaxed text-ink-soft">
          Muebles, textil, iluminación y menaje de pequeñas manufacturas del norte.
          Cada pieza viene con la historia de quien la hizo.
        </p>
        <div className="mt-10 flex flex-wrap items-center gap-3">
          <a
            href="#catalogo"
            data-focus-ring
            className="group inline-flex items-center gap-2 rounded-full bg-ink px-5 py-2.5 text-sm font-medium text-parchment transition-all hover:bg-ink-soft"
          >
            Ver catálogo
            <ArrowRight
              size={16}
              className="transition-transform group-hover:translate-x-0.5"
            />
          </a>
          <a
            href="#taller"
            data-focus-ring
            className="rounded-full border border-border px-5 py-2.5 text-sm font-medium text-ink-soft transition-colors hover:border-ink hover:text-ink"
          >
            Conoce el taller
          </a>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-6 pb-16 md:px-10">
        <StatusCard />
      </section>

      <section
        id="taller"
        className="mx-auto max-w-6xl border-t border-border px-6 py-20 md:px-10"
      >
        <p className="font-mono text-[11px] uppercase tracking-[0.28em] text-muted">
          El oficio
        </p>
        <h2 className="mt-4 max-w-2xl font-display text-3xl text-balance text-ink md:text-4xl">
          Tres razones por las que nuestras cosas duran más que una tendencia.
        </h2>
        <div className="mt-12 grid gap-10 md:grid-cols-3">
          <Pillar
            icon={Leaf}
            title="Materiales que envejecen bien"
            body="Roble, lino, cerámica, latón. Sin MDF, sin melamina, sin prisa. Si algo se rompe, se arregla."
          />
          <Pillar
            icon={Sparkles}
            title="Diseñado para usarse"
            body="Nada es decorativo por postureo. Si lo vendemos, sirve. Si solo es bonito, no lo vendemos."
          />
          <Pillar
            icon={ArrowRight}
            title="Con la cara del que lo hizo"
            body="Cada pieza llega con el nombre del artesano y de dónde es. Sin intermediarios opacos."
          />
        </div>
      </section>
    </>
  )
}
