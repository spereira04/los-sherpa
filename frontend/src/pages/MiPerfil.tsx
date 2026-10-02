import { useEffect, useState } from 'react'
import { api } from '../api/cliente'
import type { Perfil } from '../api/tipos'
import { mensajeDe, useRecurso } from '../api/useRecurso'
import { useSesion } from '../auth/SesionContext'
import { Aviso, Campo, Cargando, Tarjeta } from '../components/ui'

/** Mi perfil, para atleta y entrenador. El email y el rol se muestran pero no se editan. */
export function MiPerfil() {
  const { refrescar } = useSesion()
  const { datos, error, cargando, recargar } = useRecurso<Perfil>(
    () => api.obtener<Perfil>('/api/perfil'),
    [],
  )

  const [nombre, setNombre] = useState('')
  const [apellido, setApellido] = useState('')
  const [fechaNacimiento, setFechaNacimiento] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [pesoKg, setPesoKg] = useState('')
  const [aniosServicio, setAniosServicio] = useState('')
  const [errorGuardar, setErrorGuardar] = useState<string | null>(null)
  const [guardado, setGuardado] = useState(false)
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    if (datos === null) {
      return
    }
    setNombre(datos.nombre)
    setApellido(datos.apellido)
    setFechaNacimiento(datos.fechaNacimiento)
    setDescripcion(datos.descripcion ?? '')
    setPesoKg(datos.pesoKg === undefined ? '' : String(datos.pesoKg))
    setAniosServicio(datos.aniosServicio === undefined ? '' : String(datos.aniosServicio))
  }, [datos])

  async function guardar(evento: React.FormEvent) {
    evento.preventDefault()
    if (datos === null) {
      return
    }
    setGuardando(true)
    setErrorGuardar(null)
    setGuardado(false)
    try {
      await api.actualizar('/api/perfil', {
        nombre,
        apellido,
        fechaNacimiento,
        descripcion: descripcion.trim() === '' ? undefined : descripcion,
        pesoKg: datos.rol === 'ATLETA' ? Number(pesoKg) : undefined,
        aniosServicio: datos.rol === 'ENTRENADOR' ? Number(aniosServicio) : undefined,
      })
      setGuardado(true)
      await recargar()
      await refrescar()
    } catch (e) {
      setErrorGuardar(mensajeDe(e))
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) {
    return <Cargando />
  }
  if (error !== null || datos === null) {
    return <Aviso tipo="error">{error ?? 'No se pudo cargar el perfil'}</Aviso>
  }

  return (
    <>
      <h1>Mi perfil</h1>
      <Tarjeta>
        {errorGuardar !== null && <Aviso tipo="error">{errorGuardar}</Aviso>}
        {guardado && <Aviso tipo="ok">Perfil actualizado.</Aviso>}

        <Campo etiqueta="Email (no se puede modificar)">
          <input value={datos.email} disabled />
        </Campo>
        <p className="ayuda">
          Rol: {datos.rol === 'ATLETA' ? 'Atleta' : 'Entrenador'} · Edad: {datos.edad} anos
        </p>

        <form onSubmit={guardar}>
          <div className="fila">
            <Campo etiqueta="Nombre">
              <input value={nombre} onChange={(e) => setNombre(e.target.value)} required />
            </Campo>
            <Campo etiqueta="Apellido">
              <input value={apellido} onChange={(e) => setApellido(e.target.value)} required />
            </Campo>
          </div>
          <Campo etiqueta="Fecha de nacimiento">
            <input type="date" value={fechaNacimiento}
                   onChange={(e) => setFechaNacimiento(e.target.value)} required />
          </Campo>
          {datos.rol === 'ATLETA' ? (
            <Campo etiqueta="Peso (kg)">
              <input type="number" step="0.1" min="20" max="400" value={pesoKg}
                     onChange={(e) => setPesoKg(e.target.value)} required />
            </Campo>
          ) : (
            <Campo etiqueta="Anos de servicio">
              <input type="number" min="0" max="80" value={aniosServicio}
                     onChange={(e) => setAniosServicio(e.target.value)} required />
            </Campo>
          )}
          <Campo etiqueta="Descripcion">
            <textarea value={descripcion} onChange={(e) => setDescripcion(e.target.value)}
                      maxLength={1000} />
          </Campo>
          <button type="submit" disabled={guardando}>
            {guardando ? 'Guardando...' : 'Guardar cambios'}
          </button>
        </form>
      </Tarjeta>
    </>
  )
}
