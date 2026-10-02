import type { ReactNode } from 'react'

export function Cargando({ texto = 'Cargando...' }: { texto?: string }) {
  return <p className="cargando">{texto}</p>
}

export function Aviso({ tipo, children }: { tipo: 'error' | 'ok' | 'info'; children: ReactNode }) {
  return (
    <p className={`aviso aviso-${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      {children}
    </p>
  )
}

export function Tarjeta({ titulo, acciones, children }: {
  titulo?: string
  acciones?: ReactNode
  children: ReactNode
}) {
  return (
    <section className="tarjeta">
      {(titulo !== undefined || acciones !== undefined) && (
        <header className="tarjeta-encabezado">
          {titulo !== undefined && <h2>{titulo}</h2>}
          {acciones}
        </header>
      )}
      {children}
    </section>
  )
}

export function Campo({ etiqueta, error, children }: {
  etiqueta: string
  error?: string
  children: ReactNode
}) {
  return (
    <label className="campo">
      <span className="campo-etiqueta">{etiqueta}</span>
      {children}
      {error !== undefined && <span className="campo-error">{error}</span>}
    </label>
  )
}

export function Vacio({ children }: { children: ReactNode }) {
  return <p className="vacio">{children}</p>
}

/** Formatea una fecha ISO (yyyy-mm-dd) al formato dd/mm/aaaa. */
export function fechaCorta(iso: string | undefined): string {
  if (iso === undefined) {
    return '—'
  }
  const soloFecha = iso.slice(0, 10)
  const [anio, mes, dia] = soloFecha.split('-')
  return `${dia}/${mes}/${anio}`
}
