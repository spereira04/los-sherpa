import { useState } from 'react'
import { api } from '../../api/cliente'
import type { Ejecucion } from '../../api/tipos'
import { useRecurso } from '../../api/useRecurso'
import {
  HistorialEjecuciones,
  queryDeRango,
  type RangoFechas,
} from '../../components/HistorialEjecuciones'
import { Aviso } from '../../components/ui'

export function MiHistorial() {
  const [rango, setRango] = useState<RangoFechas>({ desde: '', hasta: '' })
  const { datos, error, cargando } = useRecurso<Ejecucion[]>(
    () => api.obtener<Ejecucion[]>(`/api/atleta/ejecuciones${queryDeRango(rango)}`),
    [rango.desde, rango.hasta],
  )

  return (
    <>
      <h1>Historial de ejecuciones</h1>
      {error !== null && <Aviso tipo="error">{error}</Aviso>}
      <HistorialEjecuciones
        titulo="Mis entrenamientos"
        ejecuciones={datos ?? []}
        cargando={cargando}
        rango={rango}
        onFiltrar={setRango}
      />
    </>
  )
}
