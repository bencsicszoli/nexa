import { useEffect, useState } from 'react'

type Health = {
  status: string
  service: string
  version: string
  timestamp: string
}

type FetchState =
  | { kind: 'loading' }
  | { kind: 'ok'; data: Health }
  | { kind: 'error'; message: string }

export default function App() {
  const [state, setState] = useState<FetchState>({ kind: 'loading' })

  useEffect(() => {
    fetch('/api/health')
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json() as Promise<Health>
      })
      .then((data) => setState({ kind: 'ok', data }))
      .catch((err) => setState({ kind: 'error', message: String(err.message ?? err) }))
  }, [])

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
      <div className="w-full max-w-md rounded-2xl bg-white shadow-lg p-8 text-center">
        <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-brand to-brand-light text-3xl font-extrabold text-white">
          N
        </div>
        <h1 className="text-2xl font-extrabold text-brand">Nexa</h1>
        <p className="mt-1 text-sm text-slate-500">
          Reklámmentes, előfizetéses közösségi platform
        </p>

        <div className="mt-6 rounded-xl border border-slate-100 bg-slate-50 p-4 text-left">
          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">
            Backend állapot
          </div>
          {state.kind === 'loading' && (
            <div className="mt-2 text-sm text-slate-500">Kapcsolódás…</div>
          )}
          {state.kind === 'ok' && (
            <div className="mt-2 flex items-center gap-2 text-sm">
              <span className="inline-block h-2.5 w-2.5 rounded-full bg-green-500" />
              <span className="font-semibold text-green-600">{state.data.status}</span>
              <span className="text-slate-400">
                · {state.data.service} v{state.data.version}
              </span>
            </div>
          )}
          {state.kind === 'error' && (
            <div className="mt-2 flex items-center gap-2 text-sm">
              <span className="inline-block h-2.5 w-2.5 rounded-full bg-red-500" />
              <span className="font-semibold text-red-600">Nincs kapcsolat</span>
              <span className="text-slate-400">· {state.message}</span>
            </div>
          )}
        </div>

        <p className="mt-6 text-xs text-slate-400">
          1. kártya — projektváz &amp; health check ✓
        </p>
      </div>
    </div>
  )
}
