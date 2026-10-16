import { useState } from 'react'
import { api } from '../../api/cliente'
import type { Ejecucion } from '../../api/tipos'
import { mensajeDe, useRecurso } from '../../api/useRecurso'
import {
  HistorialEjecuciones,
  queryDeRango,
  type RangoFechas,
} from '../../components/HistorialEjecuciones'
import { Aviso } from '../../components/ui'

export function MiHistorial() {
  const [rango, setRango] = useState<RangoFechas>({ desde: '', hasta: '' })
  const [errorEliminacion, setErrorEliminacion] = useState<string | null>(null)
  const [eliminandoId, setEliminandoId] = useState<string | null>(null)
  const [eliminandoTodo, setEliminandoTodo] = useState(false)
  const { datos, error, cargando, recargar } = useRecurso<Ejecucion[]>(
    () => api.obtener<Ejecucion[]>(`/api/atleta/ejecuciones${queryDeRango(rango)}`),
    [rango.desde, rango.hasta],
  )

  async function eliminar(idEjecucion: string) {
    const ejecucion = datos?.find((item) => item.id === idEjecucion)
    if (ejecucion === undefined
      || !window.confirm(`¿Eliminar la ejecución de "${ejecucion.nombreRutina}"?`)) {
      return
    }

    setEliminandoId(idEjecucion)
    setErrorEliminacion(null)
    try {
      await api.borrar<void>(`/api/atleta/ejecuciones/${idEjecucion}`)
      await recargar()
    } catch (e) {
      setErrorEliminacion(mensajeDe(e))
    } finally {
      setEliminandoId(null)
    }
  }

  async function eliminarTodo() {
    if (!window.confirm(
      '¿Eliminar permanentemente todas las ejecuciones de tu historial? Esta acción no se puede deshacer.',
    )) {
      return
    }

    setEliminandoTodo(true)
    setErrorEliminacion(null)
    try {
      await api.borrar<void>('/api/atleta/ejecuciones')
      await recargar()
    } catch (e) {
      setErrorEliminacion(mensajeDe(e))
    } finally {
      setEliminandoTodo(false)
    }
  }

  return (
    <>
      <h1>Historial de ejecuciones</h1>
      {error !== null && <Aviso tipo="error">{error}</Aviso>}
      {errorEliminacion !== null && <Aviso tipo="error">{errorEliminacion}</Aviso>}
      <p>
        <button
          type="button"
          className="secundario"
          disabled={eliminandoTodo || eliminandoId !== null}
          onClick={eliminarTodo}
        >
          {eliminandoTodo ? 'Eliminando historial...' : 'Borrar todo mi historial'}
        </button>
      </p>
      <HistorialEjecuciones
        titulo="Mis entrenamientos"
        ejecuciones={datos ?? []}
        cargando={cargando}
        rango={rango}
        onFiltrar={setRango}
        onEliminar={eliminar}
        eliminandoId={eliminandoId}
      />
    </>
  )
}
