import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Layout } from './components/layout/Layout.jsx'
import { Landing } from './pages/Landing.jsx'

function NotFound() {
  return (
    <section className="mx-auto flex min-h-[60vh] max-w-3xl flex-col items-start justify-center px-6 md:px-10">
      <p className="font-mono text-xs uppercase tracking-[0.28em] text-muted">404</p>
      <h1 className="mt-4 font-display text-5xl text-ink">Página no encontrada.</h1>
      <p className="mt-3 max-w-md text-ink-soft">
        Igual se ha movido de sitio. Vuelve al inicio y echa un ojo al catálogo.
      </p>
      <a
        href="/"
        data-focus-ring
        className="mt-6 inline-flex items-center gap-2 rounded-full bg-ink px-5 py-2.5 text-sm text-parchment hover:bg-ink-soft"
      >
        ← Volver
      </a>
    </section>
  )
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Landing />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
