import { useState } from 'react'
import { api } from '../../api/cliente'
import type { Rutina } from '../../api/tipos'
import { mensajeDe } from '../../api/useRecurso'
import { Aviso, Campo, Tarjeta } from '../../components/ui'

type Cargas = Record<string, string>

function clave(idEjercicio: string, nroSerie: number): string {
  return `${idEjercicio}#${nroSerie}`
}

/** Carga de los kg de cada serie de una rutina. Se envia como una ejecucion nueva. */
export function CompletarRutina({ rutina, onCerrar, onListo }: {
  rutina: Rutina
  onCerrar: () => void
  onListo: () => Promise<void>
}) {
  const hoy = new Date().toISOString().slice(0, 10)
  const [fecha, setFecha] = useState(hoy)
  const [cargas, setCargas] = useState<Cargas>({})
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)

    const series = rutina.ejercicios.flatMap((ejercicio) =>
      Array.from({ length: ejercicio.series }, (_, i) => ({
        idEjercicio: ejercicio.id,
        nroSerie: i + 1,
        cargaKg: Number(cargas[clave(ejercicio.id, i + 1)] ?? '0'),
      })),
    )

    try {
      await api.crear(`/api/atleta/rutinas/${rutina.id}/ejecuciones`, { fecha, series })
      await onListo()
    } catch (e) {
      setError(mensajeDe(e))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Tarjeta
      titulo={`Completar: ${rutina.nombre}`}
      acciones={
        <button type="button" className="secundario" onClick={onCerrar}>
          Cerrar
        </button>
      }
    >
      {error !== null && <Aviso tipo="error">{error}</Aviso>}
      <form onSubmit={enviar}>
        <Campo etiqueta="Fecha">
          <input type="date" value={fecha} max={hoy} required
                 onChange={(e) => setFecha(e.target.value)} />
        </Campo>

        {rutina.ejercicios.map((ejercicio) => (
          <div key={ejercicio.id} style={{ marginBottom: '0.8rem' }}>
            <h3>
              {ejercicio.nombre}{' '}
              <span className="ayuda">({ejercicio.repeticiones} repeticiones por serie)</span>
            </h3>
            <div className="fila">
              {Array.from({ length: ejercicio.series }, (_, i) => i + 1).map((nroSerie) => (
                <Campo key={nroSerie} etiqueta={`Serie ${nroSerie} (kg)`}>
                  <input
                    type="number" step="0.5" min="0" max="1000" required
                    value={cargas[clave(ejercicio.id, nroSerie)] ?? ''}
                    onChange={(e) =>
                      setCargas((previas) => ({
                        ...previas,
                        [clave(ejercicio.id, nroSerie)]: e.target.value,
                      }))
                    }
                  />
                </Campo>
              ))}
            </div>
          </div>
        ))}

        <button type="submit" disabled={enviando}>
          {enviando ? 'Guardando...' : 'Guardar ejecucion'}
        </button>
        <p className="ayuda">Hay que cargar la carga de todas las series.</p>
      </form>
    </Tarjeta>
  )
}
