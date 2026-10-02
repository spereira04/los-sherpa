import { useState } from 'react'
import type { Ejecucion } from '../api/tipos'
import { Campo, Cargando, Tarjeta, Vacio, fechaCorta } from './ui'

export type RangoFechas = { desde: string; hasta: string }

/**
 * Historial de ejecuciones con filtro por rango de fechas.
 * Lo usan el atleta (su historial) y el entrenador (el de su atleta activo).
 */
export function HistorialEjecuciones({ titulo, ejecuciones, cargando, rango, onFiltrar }: {
  titulo: string
  ejecuciones: Ejecucion[]
  cargando: boolean
  rango: RangoFechas
  onFiltrar: (rango: RangoFechas) => void
}) {
  const [desde, setDesde] = useState(rango.desde)
  const [hasta, setHasta] = useState(rango.hasta)

  return (
    <Tarjeta titulo={titulo}>
      <form
        className="fila"
        onSubmit={(e) => {
          e.preventDefault()
          onFiltrar({ desde, hasta })
        }}
      >
        <Campo etiqueta="Desde">
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
        </Campo>
        <Campo etiqueta="Hasta">
          <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
        </Campo>
        <button type="submit">Filtrar</button>
        {(desde !== '' || hasta !== '') && (
          <button type="button" className="secundario"
                  onClick={() => { setDesde(''); setHasta(''); onFiltrar({ desde: '', hasta: '' }) }}>
            Limpiar
          </button>
        )}
      </form>

      {cargando ? (
        <Cargando />
      ) : ejecuciones.length === 0 ? (
        <Vacio>No hay ejecuciones en ese rango.</Vacio>
      ) : (
        ejecuciones.map((ejecucion) => (
          <div key={ejecucion.id} style={{ marginBottom: '1rem' }}>
            <h3>
              {ejecucion.nombreRutina}{' '}
              <span className="ayuda">· {fechaCorta(ejecucion.fecha)}</span>
            </h3>
            <table>
              <thead>
                <tr>
                  <th>Ejercicio</th>
                  <th>Serie</th>
                  <th>Carga (kg)</th>
                </tr>
              </thead>
              <tbody>
                {ejecucion.series.map((serie) => (
                  <tr key={`${serie.idEjercicio}#${serie.nroSerie}`}>
                    <td>{serie.nombreEjercicio}</td>
                    <td>{serie.nroSerie}</td>
                    <td>{serie.cargaKg}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))
      )}
    </Tarjeta>
  )
}

/** Arma el query string del filtro, omitiendo los extremos vacios. */
export function queryDeRango(rango: RangoFechas): string {
  const partes: string[] = []
  if (rango.desde !== '') {
    partes.push(`desde=${rango.desde}`)
  }
  if (rango.hasta !== '') {
    partes.push(`hasta=${rango.hasta}`)
  }
  return partes.length === 0 ? '' : `?${partes.join('&')}`
}
