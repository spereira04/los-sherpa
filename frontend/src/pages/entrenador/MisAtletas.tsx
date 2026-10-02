import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/cliente'
import type { AtletaActivo, Exatleta, SolicitudRecibida } from '../../api/tipos'
import { mensajeDe, useRecurso } from '../../api/useRecurso'
import { Aviso, Cargando, Tarjeta, Vacio, fechaCorta } from '../../components/ui'

export function MisAtletas() {
  const atletas = useRecurso<AtletaActivo[]>(
    () => api.obtener<AtletaActivo[]>('/api/entrenador/atletas'),
    [],
  )
  const solicitudes = useRecurso<SolicitudRecibida[]>(
    () => api.obtener<SolicitudRecibida[]>('/api/entrenador/solicitudes'),
    [],
  )
  const exatletas = useRecurso<Exatleta[]>(
    () => api.obtener<Exatleta[]>('/api/entrenador/exatletas'),
    [],
  )
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  async function resolver(idSolicitud: string, accion: 'aceptar' | 'rechazar') {
    setOcupado(true)
    setError(null)
    try {
      await api.crear(`/api/entrenador/solicitudes/${idSolicitud}/${accion}`)
      await Promise.all([atletas.recargar(), solicitudes.recargar()])
    } catch (e) {
      setError(mensajeDe(e))
    } finally {
      setOcupado(false)
    }
  }

  return (
    <>
      <h1>Mis atletas</h1>
      {error !== null && <Aviso tipo="error">{error}</Aviso>}

      <Tarjeta titulo="Solicitudes pendientes">
        {solicitudes.cargando ? (
          <Cargando />
        ) : (solicitudes.datos ?? []).length === 0 ? (
          <Vacio>No tenes solicitudes pendientes.</Vacio>
        ) : (
          <ul className="lista">
            {(solicitudes.datos ?? []).map((solicitud) => (
              <li key={solicitud.id}>
                <div className="fila">
                  <div>
                    <strong>{solicitud.atleta.nombre} {solicitud.atleta.apellido}</strong>
                    <div className="ayuda">
                      {solicitud.atleta.edad} anos · recibida el {fechaCorta(solicitud.creadaEn)}
                    </div>
                  </div>
                  <button type="button" disabled={ocupado}
                          onClick={() => void resolver(solicitud.id, 'aceptar')}>
                    Aceptar
                  </button>
                  <button type="button" className="peligro" disabled={ocupado}
                          onClick={() => void resolver(solicitud.id, 'rechazar')}>
                    Rechazar
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Tarjeta>

      <Tarjeta titulo="Atletas activos">
        {atletas.cargando ? (
          <Cargando />
        ) : (atletas.datos ?? []).length === 0 ? (
          <Vacio>Todavia no tenes atletas.</Vacio>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Atleta</th>
                <th>Edad</th>
                <th>Peso</th>
                <th>Desde</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {(atletas.datos ?? []).map(({ atleta, desde }) => (
                <tr key={atleta.id}>
                  <td>{atleta.nombre} {atleta.apellido}</td>
                  <td>{atleta.edad}</td>
                  <td>{atleta.pesoKg ?? '—'} kg</td>
                  <td>{fechaCorta(desde)}</td>
                  <td className="acciones">
                    <Link to={`/entrenador/atletas/${atleta.id}`}>Ver detalle</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Tarjeta>

      <Tarjeta titulo="Historial de exatletas">
        {exatletas.cargando ? (
          <Cargando />
        ) : (exatletas.datos ?? []).length === 0 ? (
          <Vacio>No tenes exatletas.</Vacio>
        ) : (
          <>
            <table>
              <thead>
                <tr>
                  <th>Exatleta</th>
                  <th>Desde</th>
                  <th>Hasta</th>
                </tr>
              </thead>
              <tbody>
                {(exatletas.datos ?? []).map((exatleta, indice) => (
                  <tr key={indice}>
                    <td>{exatleta.nombre} {exatleta.apellido}</td>
                    <td>{fechaCorta(exatleta.fechaInicio)}</td>
                    <td>{fechaCorta(exatleta.fechaFin)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="ayuda">
              Del historial solo quedan el nombre y las fechas del vinculo.
            </p>
          </>
        )}
      </Tarjeta>
    </>
  )
}
