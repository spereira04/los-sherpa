import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../../api/cliente'
import type { Ejecucion, PerfilPublico, Rutina } from '../../api/tipos'
import { mensajeDe, useRecurso } from '../../api/useRecurso'
import { FormularioRutina, type EjercicioNuevo } from '../../components/FormularioRutina'
import {
  HistorialEjecuciones,
  queryDeRango,
  type RangoFechas,
} from '../../components/HistorialEjecuciones'
import { Aviso, Cargando, Tarjeta, Vacio, fechaCorta } from '../../components/ui'

export function DetalleAtleta() {
  const { idAtleta = '' } = useParams()
  const navegar = useNavigate()
  const [rango, setRango] = useState<RangoFechas>({ desde: '', hasta: '' })

  const atleta = useRecurso<PerfilPublico>(
    () => api.obtener<PerfilPublico>(`/api/entrenador/atletas/${idAtleta}`),
    [idAtleta],
  )
  const rutinas = useRecurso<Rutina[]>(
    () => api.obtener<Rutina[]>(`/api/entrenador/atletas/${idAtleta}/rutinas`),
    [idAtleta],
  )
  const ejecuciones = useRecurso<Ejecucion[]>(
    () => api.obtener<Ejecucion[]>(
      `/api/entrenador/atletas/${idAtleta}/ejecuciones${queryDeRango(rango)}`),
    [idAtleta, rango.desde, rango.hasta],
  )

  const [creando, setCreando] = useState(false)
  const [errorCrear, setErrorCrear] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [errorAccion, setErrorAccion] = useState<string | null>(null)

  async function crearRutina(nombre: string, ejercicios: EjercicioNuevo[]) {
    setEnviando(true)
    setErrorCrear(null)
    try {
      await api.crear(`/api/entrenador/atletas/${idAtleta}/rutinas`, { nombre, ejercicios })
      setCreando(false)
      await rutinas.recargar()
    } catch (e) {
      setErrorCrear(mensajeDe(e))
    } finally {
      setEnviando(false)
    }
  }

  async function desvincular() {
    setErrorAccion(null)
    try {
      await api.borrar(`/api/entrenador/atletas/${idAtleta}/vinculo`)
      navegar('/entrenador/atletas', { replace: true })
    } catch (e) {
      setErrorAccion(mensajeDe(e))
    }
  }

  if (atleta.cargando) {
    return <Cargando />
  }
  if (atleta.error !== null || atleta.datos === null) {
    return (
      <Aviso tipo="error">
        {atleta.error ?? 'Este atleta no existe o ya no esta vinculado con vos.'}
      </Aviso>
    )
  }

  return (
    <>
      <h1>{atleta.datos.nombre} {atleta.datos.apellido}</h1>
      {errorAccion !== null && <Aviso tipo="error">{errorAccion}</Aviso>}

      <Tarjeta
        titulo="Datos del atleta"
        acciones={
          <button type="button" className="peligro" onClick={() => void desvincular()}>
            Desvincular
          </button>
        }
      >
        <p>
          Edad: {atleta.datos.edad} anos · Peso: {atleta.datos.pesoKg ?? '—'} kg
        </p>
        <p>{atleta.datos.descripcion ?? 'Sin descripcion.'}</p>
      </Tarjeta>

      <Tarjeta
        titulo="Rutinas que le asigne"
        acciones={
          <button type="button" className="secundario" onClick={() => setCreando(!creando)}>
            {creando ? 'Cancelar' : 'Crear rutina'}
          </button>
        }
      >
        {creando && (
          <FormularioRutina onGuardar={crearRutina} enviando={enviando} error={errorCrear} />
        )}

        {rutinas.cargando ? (
          <Cargando />
        ) : (rutinas.datos ?? []).length === 0 ? (
          <Vacio>Todavia no le asignaste rutinas.</Vacio>
        ) : (
          (rutinas.datos ?? []).map((rutina) => (
            <div key={rutina.id} style={{ marginBottom: '1rem' }}>
              <h3>
                {rutina.nombre}{' '}
                <span className="ayuda">· creada el {fechaCorta(rutina.creadaEn)}</span>
              </h3>
              <table>
                <thead>
                  <tr>
                    <th>Ejercicio</th>
                    <th>Series</th>
                    <th>Repeticiones</th>
                  </tr>
                </thead>
                <tbody>
                  {rutina.ejercicios.map((ejercicio) => (
                    <tr key={ejercicio.id}>
                      <td>{ejercicio.nombre}</td>
                      <td>{ejercicio.series}</td>
                      <td>{ejercicio.repeticiones}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))
        )}
        <p className="ayuda">
          Solo ves las rutinas que vos le asignaste. Las que el atleta crea para si mismo no
          se muestran.
        </p>
      </Tarjeta>

      <HistorialEjecuciones
        titulo="Historial de las rutinas que le asigne"
        ejecuciones={ejecuciones.datos ?? []}
        cargando={ejecuciones.cargando}
        rango={rango}
        onFiltrar={setRango}
      />
    </>
  )
}
