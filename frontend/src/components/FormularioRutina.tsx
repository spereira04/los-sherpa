import { useEffect, useState } from 'react'
import { Aviso, Campo } from './ui'

export type EjercicioNuevo = {
  nombre: string
  series: number
  repeticiones: number
  observaciones?: string
}

export function FormularioRutina({
  onGuardar,
  enviando,
  error,
  valorInicial,
  textoBoton = 'Crear rutina',
}: {
  onGuardar: (nombre: string, ejercicios: EjercicioNuevo[]) => void
  enviando: boolean
  error: string | null
  valorInicial?: { nombre: string; ejercicios: EjercicioNuevo[] }
  textoBoton?: string
}) {
  const [nombre, setNombre] = useState(valorInicial?.nombre ?? '')
  const [ejercicios, setEjercicios] = useState<EjercicioNuevo[]>(
    valorInicial?.ejercicios.length
      ? valorInicial.ejercicios
      : [{ nombre: '', series: 3, repeticiones: 10, observaciones: '' }],
  )

  useEffect(() => {
    if (valorInicial) {
      setNombre(valorInicial.nombre)
      setEjercicios(valorInicial.ejercicios)
    }
  }, [valorInicial])

  function cambiar(indice: number, cambios: Partial<EjercicioNuevo>) {
    setEjercicios((previos) =>
      previos.map((ejercicio, i) => (i === indice ? { ...ejercicio, ...cambios } : ejercicio)),
    )
  }

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        onGuardar(nombre, ejercicios)
      }}
    >
      {error !== null && <Aviso tipo="error">{error}</Aviso>}

      <Campo etiqueta="Nombre de la rutina">
        <input value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={120}
               required />
      </Campo>

      <h3>Ejercicios</h3>
      {ejercicios.map((ejercicio, indice) => (
        <div className="fila" key={indice}>
          <Campo etiqueta="Ejercicio">
            <input value={ejercicio.nombre} maxLength={120} required
                   onChange={(e) => cambiar(indice, { nombre: e.target.value })} />
          </Campo>
          <Campo etiqueta="Series">
            <input type="number" min={1} max={20} value={ejercicio.series} required
                   onChange={(e) => cambiar(indice, { series: Number(e.target.value) })} />
          </Campo>
          <Campo etiqueta="Repeticiones">
            <input type="number" min={1} max={500} value={ejercicio.repeticiones} required
                   onChange={(e) => cambiar(indice, { repeticiones: Number(e.target.value) })} />
          </Campo>
          <Campo etiqueta="Observaciones técnicas">
            <textarea value={ejercicio.observaciones ?? ''} maxLength={500}
                      onChange={(e) => cambiar(indice, { observaciones: e.target.value })} />
          </Campo>
          {ejercicios.length > 1 && (
            <button type="button" className="peligro"
                    onClick={() => setEjercicios((p) => p.filter((_, i) => i !== indice))}>
              Quitar
            </button>
          )}
        </div>
      ))}

      <div className="fila" style={{ marginTop: '0.5rem' }}>
        <button
          type="button"
          className="secundario"
          disabled={ejercicios.length >= 30}
          onClick={() =>
            setEjercicios((p) => [...p, { nombre: '', series: 3, repeticiones: 10, observaciones: '' }])
          }
        >
          Agregar ejercicio
        </button>
        <button type="submit" disabled={enviando}>
          {enviando ? 'Guardando...' : textoBoton}
        </button>
      </div>
      <p className="ayuda">
        Las observaciones técnicas quedan visibles para el atleta y se guardan junto con cada ejercicio.
      </p>
    </form>
  )
}
