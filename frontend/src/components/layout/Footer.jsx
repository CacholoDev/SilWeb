export function Footer() {
  return (
    <footer className="mt-24 border-t border-border/70 bg-parchment-deep/40">
      <div className="mx-auto flex max-w-6xl flex-col items-start justify-between gap-3 px-6 py-8 text-xs text-muted md:flex-row md:items-center md:px-10">
        <p className="font-mono">
          &copy; {new Date().getFullYear()} Silvalde · Hecho en Galicia
        </p>
        <p className="font-mono tracking-wide">v0.1 · Fase 1</p>
      </div>
    </footer>
  )
}
