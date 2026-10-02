import { useState } from 'react'
import { api } from '../../api/cliente'
import type { MiEntrenadorDato, PerfilPublico, SolicitudEnviada } from './tipos'
import { mensajeDe, useRecurso } from '../../api/useRecurso'
import { Aviso, Campo, Cargando, Tarjeta, Vacio, fechaCorta } from '../../components/ui'

/**
 * Si el atleta tiene entrenador, muestra su perfil y la opcion de desvincular.
 * Si no, muestra el listado de entrenadores con busqueda y las solicitudes enviadas.
 */
export function MiEntrenador() {
  const vinculo = useRecurso<MiEntrenadorDato>(
    () => api.obtener<MiEntrenadorDato>('/api/atleta/entrenador'),
    [],
    { opcional: true },
  )
  const solicitudes = useRecurso<SolicitudEnviada[]>(
    () => api.obtener<SolicitudEnviada[]>('/api/atleta/solicitudes'),
    [],
  )
  const [busqueda, setBusqueda] = useState('')
  const [filtro, setFiltro] = useState('')
  const entrenadores = useRecurso<PerfilPublico[]>(
    () => api.obtener<PerfilPublico[]>(
      `/api/atleta/entrenadores${filtro === '' ? '' : `?nombre=${encodeURIComponent(filtro)}`}`),
    [filtro],
  )

  const [accionError, setAccionError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  async function ejecutar(accion: () => Promise<unknown>) {
    setOcupado(true)
    setAccionError(null)
    try {
      await accion()
      await Promise.all([vinculo.recargar(), solicitudes.recargar(), entrenadores.recargar()])
    } catch (e) {
      setAccionError(mensajeDe(e))
    } finally {
      setOcupado(false)
    }
  }

  if (vinculo.cargando) {
    return <Cargando />
  }

  const pendientes = (solicitudes.datos ?? []).filter((s) => s.estado === 'PENDIENTE')
  const resueltas = (solicitudes.datos ?? []).filter((s) => s.estado !== 'PENDIENTE')

  return (
    <>
      <h1>Mi entrenador</h1>
      {accionError !== null && <Aviso tipo="error">{accionError}</Aviso>}

      {vinculo.datos !== null ? (
        <Tarjeta
          titulo={`${vinculo.datos.entrenador.nombre} ${vinculo.datos.entrenador.apellido}`}
          acciones={
            <button type="button" className="peligro" disabled={ocupado}
                    onClick={() => void ejecutar(() => api.borrar('/api/atleta/vinculo'))}>
              Desvincularme
            </button>
          }
        >
          <p>
            Edad: {vinculo.datos.entrenador.edad} anos · Anos de servicio:{' '}
            {vinculo.datos.entrenador.aniosServicio ?? '—'}
          </p>
          <p>{vinculo.datos.entrenador.descripcion ?? 'Sin descripcion.'}</p>
          <p className="ayuda">Vinculados desde el {fechaCorta(vinculo.datos.desde)}.</p>
        </Tarjeta>
      ) : (
        <>
          <Tarjeta titulo="Buscar un entrenador">
            <form
              className="fila"
              onSubmit={(e) => {
                e.preventDefault()
                setFiltro(busqueda.trim())
              }}
            >
              <Campo etiqueta="Nombre o apellido">
                <input value={busqueda} onChange={(e) => setBusqueda(e.target.value)}
                       placeholder="Ej.: Lopez" />
              </Campo>
              <button type="submit">Buscar</button>
              {filtro !== '' && (
                <button type="button" className="secundario"
                        onClick={() => { setBusqueda(''); setFiltro('') }}>
                  Limpiar
                </button>
              )}
            </form>

            {entrenadores.cargando ? (
              <Cargando />
            ) : (entrenadores.datos ?? []).length === 0 ? (
              <Vacio>No se encontraron entrenadores.</Vacio>
            ) : (
              <ul className="lista">
                {(entrenadores.datos ?? []).map((entrenador) => {
                  const yaSolicitado = pendientes.some((s) => s.entrenador.id === entrenador.id)
                  return (
                    <li key={entrenador.id}>
                      <div className="fila">
                        <div>
                          <strong>{entrenador.nombre} {entrenador.apellido}</strong>
                          <div className="ayuda">
                            {entrenador.edad} anos · {entrenador.aniosServicio ?? 0} anos de
                            servicio
                          </div>
                          {entrenador.descripcion !== undefined && (
                            <div>{entrenador.descripcion}</div>
                          )}
                        </div>
                        <button
                          type="button"
                          disabled={ocupado || yaSolicitado}
                          onClick={() => void ejecutar(() =>
                            api.crear('/api/atleta/solicitudes',
                              { idEntrenador: entrenador.id }))}
                        >
                          {yaSolicitado ? 'Solicitud enviada' : 'Enviar solicitud'}
                        </button>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </Tarjeta>

          <Tarjeta titulo="Solicitudes enviadas">
            {pendientes.length === 0 ? (
              <Vacio>No tenes solicitudes pendientes.</Vacio>
            ) : (
              <ul className="lista">
                {pendientes.map((solicitud) => (
                  <li key={solicitud.id}>
                    <div className="fila">
                      <div>
                        <strong>
                          {solicitud.entrenador.nombre} {solicitud.entrenador.apellido}
                        </strong>
                        <div className="ayuda">
                          Enviada el {fechaCorta(solicitud.creadaEn)} ·{' '}
                          <span className="etiqueta">Pendiente</span>
                        </div>
                      </div>
                      <button type="button" className="peligro" disabled={ocupado}
                              onClick={() => void ejecutar(() => api.crear(
                                `/api/atleta/solicitudes/${solicitud.id}/cancelar`))}>
                        Cancelar
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            {resueltas.length > 0 && (
              <>
                <h3 style={{ marginTop: '1.2rem' }}>Resueltas</h3>
                <ul className="lista">
                  {resueltas.map((solicitud) => (
                    <li key={solicitud.id}>
                      {solicitud.entrenador.nombre} {solicitud.entrenador.apellido} ·{' '}
                      <span className="etiqueta">{solicitud.estado.toLowerCase()}</span>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </Tarjeta>
        </>
      )}
    </>
  )
}
