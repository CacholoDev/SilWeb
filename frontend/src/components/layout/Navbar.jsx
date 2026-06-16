import { HealthBadge } from '../common/HealthBadge.jsx'

export function Navbar() {
  return (
    <header className="sticky top-0 z-30 border-b border-border/70 bg-parchment/80 backdrop-blur supports-[backdrop-filter]:bg-parchment/65">
      <div className="mx-auto flex h-18 max-w-6xl items-center justify-between px-6 py-4 md:px-10">
        <a
          href="/"
          data-focus-ring
          className="group inline-flex items-baseline gap-2"
          aria-label="Silvalde — inicio"
        >
          <span className="font-display text-2xl font-medium italic tracking-tight text-ink">
            Silvalde
          </span>
          <span className="hidden text-[10px] uppercase tracking-[0.22em] text-muted sm:inline">
            hogar · 1924
          </span>
        </a>

        <nav aria-label="Principal" className="hidden md:block">
          <ul className="flex items-center gap-8 text-sm text-ink-soft">
            <li>
              <a
                href="#catalogo"
                data-focus-ring
                className="relative py-1 transition-colors hover:text-ink"
              >
                Catálogo
                <span className="absolute -bottom-0.5 left-0 h-px w-0 bg-ink transition-all group-hover:w-full" />
              </a>
            </li>
            <li>
              <a
                href="#taller"
                data-focus-ring
                className="transition-colors hover:text-ink"
              >
                Taller
              </a>
            </li>
            <li>
              <a
                href="#contacto"
                data-focus-ring
                className="transition-colors hover:text-ink"
              >
                Contacto
              </a>
            </li>
          </ul>
        </nav>

        <HealthBadge />
      </div>
    </header>
  )
}
