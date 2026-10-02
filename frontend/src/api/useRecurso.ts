import { useCallback, useEffect, useState } from 'react'
import { ErrorApi } from './cliente'

type Estado<T> = {
  datos: T | null
  error: string | null
  cargando: boolean
  recargar: () => Promise<void>
}

/**
 * Carga un recurso del backend y expone estado de carga, error y recarga.
 * Si el backend contesta 404 con `opcional`, se trata como "no hay dato" y no como error.
 */
export function useRecurso<T>(
  cargar: () => Promise<T>,
  deps: unknown[],
  opciones: { opcional?: boolean } = {},
): Estado<T> {
  const [datos, setDatos] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(true)

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const cargarMemo = useCallback(cargar, deps)

  const recargar = useCallback(async () => {
    setCargando(true)
    setError(null)
    try {
      setDatos(await cargarMemo())
    } catch (e) {
      if (opciones.opcional === true && e instanceof ErrorApi && e.estado === 404) {
        setDatos(null)
      } else {
        setError(e instanceof Error ? e.message : 'No se pudo cargar la informacion')
      }
    } finally {
      setCargando(false)
    }
  }, [cargarMemo, opciones.opcional])

  useEffect(() => {
    void recargar()
  }, [recargar])

  return { datos, error, cargando, recargar }
}

/** Mensaje de error listo para mostrar, con los errores de campo si los hubiera. */
export function mensajeDe(e: unknown): string {
  if (e instanceof ErrorApi) {
    if (e.campos !== undefined) {
      const detalles = Object.values(e.campos).join('. ')
      return detalles.length > 0 ? detalles : e.message
    }
    return e.message
  }
  return 'Ocurrio un error inesperado'
}
