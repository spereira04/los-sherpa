import { useState } from 'react'
import { api } from '../../api/cliente'
import type { Rutina } from '../../api/tipos'
import { mensajeDe, useRecurso } from '../../api/useRecurso'
import { FormularioRutina, type EjercicioNuevo } from '../../components/FormularioRutina'
import { Aviso, Cargando, Tarjeta, Vacio, fechaCorta } from '../../components/ui'
import { CompletarRutina } from './CompletarRutina'

export function MisRutinas() {
  const { datos, error, cargando, recargar } = useRecurso<Rutina[]>(
    () => api.obtener<Rutina[]>('/api/atleta/rutinas'),
    [],
  )
  const [creando, setCreando] = useState(false)
  const [errorCrear, setErrorCrear] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [completando, setCompletando] = useState<Rutina | null>(null)

  async function crear(nombre: string, ejercicios: EjercicioNuevo[]) {
    setEnviando(true)
    setErrorCrear(null)
    try {
      await api.crear('/api/atleta/rutinas', { nombre, ejercicios })
      setCreando(false)
      await recargar()
    } catch (e) {
      setErrorCrear(mensajeDe(e))
    } finally {
      setEnviando(false)
    }
  }

  if (cargando) {
    return <Cargando />
  }

  return (
    <>
      <h1>Mis rutinas</h1>
      {error !== null && <Aviso tipo="error">{error}</Aviso>}

      {completando !== null && (
        <CompletarRutina
          rutina={completando}
          onCerrar={() => setCompletando(null)}
          onListo={async () => {
            setCompletando(null)
            await recargar()
          }}
        />
      )}

      <Tarjeta
        titulo="Crear una rutina propia"
        acciones={
          <button type="button" className="secundario" onClick={() => setCreando(!creando)}>
            {creando ? 'Cancelar' : 'Nueva rutina'}
          </button>
        }
      >
        {creando ? (
          <FormularioRutina onGuardar={crear} enviando={enviando} error={errorCrear} />
        ) : (
          <p className="ayuda">
            Tu entrenador no ve las rutinas que creas vos, ni sus ejecuciones.
          </p>
        )}
      </Tarjeta>

      {(datos ?? []).length === 0 ? (
        <Vacio>Todavia no tenes rutinas.</Vacio>
      ) : (
        (datos ?? []).map((rutina) => (
          <Tarjeta
            key={rutina.id}
            titulo={rutina.nombre}
            acciones={
              <button type="button" onClick={() => setCompletando(rutina)}>
                Completar
              </button>
            }
          >
            <p className="ayuda">
              <span className="etiqueta">
                {rutina.origen === 'ASIGNADA' ? 'Asignada' : 'Propia'}
              </span>{' '}
              {rutina.creadaPor !== undefined && `por ${rutina.creadaPor} · `}
              Creada el {fechaCorta(rutina.creadaEn)}
            </p>
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
          </Tarjeta>
        ))
      )}
    </>
  )
}
