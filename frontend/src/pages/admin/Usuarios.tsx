import { useState } from 'react'
import { api } from '../../api/cliente'
import type { UsuarioAdmin } from '../../api/tipos'
import { mensajeDe, useRecurso } from '../../api/useRecurso'
import { Aviso, Campo, Cargando, Tarjeta, Vacio, fechaCorta } from '../../components/ui'

type RolElegible = 'ATLETA' | 'ENTRENADOR'

export function Usuarios() {
  const [rol, setRol] = useState('')
  const [texto, setTexto] = useState('')
  const [filtros, setFiltros] = useState({ rol: '', texto: '' })

  const usuarios = useRecurso<UsuarioAdmin[]>(() => {
    const partes: string[] = []
    if (filtros.rol !== '') {
      partes.push(`rol=${filtros.rol}`)
    }
    if (filtros.texto !== '') {
      partes.push(`q=${encodeURIComponent(filtros.texto)}`)
    }
    const query = partes.length === 0 ? '' : `?${partes.join('&')}`
    return api.obtener<UsuarioAdmin[]>(`/api/admin/usuarios${query}`)
  }, [filtros.rol, filtros.texto])

  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [creando, setCreando] = useState(false)
  const [editando, setEditando] = useState<UsuarioAdmin | null>(null)

  async function eliminar(usuario: UsuarioAdmin) {
    const confirmado = window.confirm(
      `Eliminar a ${usuario.nombre} ${usuario.apellido}? Se pierden sus rutinas propias y su `
      + 'historial, y se cierra su sesion. No se puede deshacer.',
    )
    if (!confirmado) {
      return
    }
    setError(null)
    setAviso(null)
    try {
      await api.borrar(`/api/admin/usuarios/${usuario.id}`)
      setAviso('Usuario eliminado.')
      await usuarios.recargar()
    } catch (e) {
      setError(mensajeDe(e))
    }
  }

  return (
    <>
      <h1>Gestion de usuarios</h1>
      {error !== null && <Aviso tipo="error">{error}</Aviso>}
      {aviso !== null && <Aviso tipo="ok">{aviso}</Aviso>}

      <Tarjeta
        titulo="Alta de usuario"
        acciones={
          <button type="button" className="secundario" onClick={() => setCreando(!creando)}>
            {creando ? 'Cancelar' : 'Nuevo usuario'}
          </button>
        }
      >
        {creando ? (
          <FormularioUsuario
            onListo={async () => {
              setCreando(false)
              setAviso('Usuario creado.')
              await usuarios.recargar()
            }}
          />
        ) : (
          <p className="ayuda">Solo se pueden crear entrenadores y atletas.</p>
        )}
      </Tarjeta>

      {editando !== null && (
        <Tarjeta
          titulo={`Editar: ${editando.nombre} ${editando.apellido}`}
          acciones={
            <button type="button" className="secundario" onClick={() => setEditando(null)}>
              Cerrar
            </button>
          }
        >
          <FormularioEdicion
            usuario={editando}
            onListo={async () => {
              setEditando(null)
              setAviso('Usuario actualizado.')
              await usuarios.recargar()
            }}
          />
        </Tarjeta>
      )}

      <Tarjeta titulo="Usuarios">
        <form
          className="fila"
          onSubmit={(e) => {
            e.preventDefault()
            setFiltros({ rol, texto: texto.trim() })
          }}
        >
          <Campo etiqueta="Rol">
            <select value={rol} onChange={(e) => setRol(e.target.value)}>
              <option value="">Todos</option>
              <option value="ENTRENADOR">Entrenadores</option>
              <option value="ATLETA">Atletas</option>
            </select>
          </Campo>
          <Campo etiqueta="Buscar">
            <input value={texto} onChange={(e) => setTexto(e.target.value)}
                   placeholder="Nombre, apellido o email" />
          </Campo>
          <button type="submit">Filtrar</button>
        </form>

        {usuarios.cargando ? (
          <Cargando />
        ) : (usuarios.datos ?? []).length === 0 ? (
          <Vacio>No hay usuarios para ese filtro.</Vacio>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Email</th>
                <th>Rol</th>
                <th>Alta</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {(usuarios.datos ?? []).map((usuario) => (
                <tr key={usuario.id}>
                  <td>{usuario.nombre} {usuario.apellido}</td>
                  <td>{usuario.email}</td>
                  <td><span className="etiqueta">{usuario.rol.toLowerCase()}</span></td>
                  <td>{fechaCorta(usuario.creadoEn)}</td>
                  <td className="acciones">
                    <button type="button" className="secundario"
                            onClick={() => setEditando(usuario)}>
                      Editar
                    </button>
                    <button type="button" className="peligro"
                            onClick={() => void eliminar(usuario)}>
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Tarjeta>
    </>
  )
}

function FormularioUsuario({ onListo }: { onListo: () => Promise<void> }) {
  const [rol, setRol] = useState<RolElegible>('ATLETA')
  const [email, setEmail] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [nombre, setNombre] = useState('')
  const [apellido, setApellido] = useState('')
  const [fechaNacimiento, setFechaNacimiento] = useState('')
  const [pesoKg, setPesoKg] = useState('')
  const [aniosServicio, setAniosServicio] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await api.crear('/api/admin/usuarios', {
        email,
        contrasena,
        rol,
        nombre,
        apellido,
        fechaNacimiento,
        pesoKg: rol === 'ATLETA' ? Number(pesoKg) : undefined,
        aniosServicio: rol === 'ENTRENADOR' ? Number(aniosServicio) : undefined,
      })
      await onListo()
    } catch (e) {
      setError(mensajeDe(e))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form onSubmit={enviar}>
      {error !== null && <Aviso tipo="error">{error}</Aviso>}
      <div className="fila">
        <Campo etiqueta="Rol">
          <select value={rol} onChange={(e) => setRol(e.target.value as RolElegible)}>
            <option value="ATLETA">Atleta</option>
            <option value="ENTRENADOR">Entrenador</option>
          </select>
        </Campo>
        <Campo etiqueta="Email">
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </Campo>
      </div>
      <Campo etiqueta="Contrasena">
        <input type="password" value={contrasena} minLength={12} required
               onChange={(e) => setContrasena(e.target.value)} autoComplete="new-password" />
      </Campo>
      <div className="fila">
        <Campo etiqueta="Nombre">
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} required />
        </Campo>
        <Campo etiqueta="Apellido">
          <input value={apellido} onChange={(e) => setApellido(e.target.value)} required />
        </Campo>
      </div>
      <div className="fila">
        <Campo etiqueta="Fecha de nacimiento">
          <input type="date" value={fechaNacimiento} required
                 onChange={(e) => setFechaNacimiento(e.target.value)} />
        </Campo>
        {rol === 'ATLETA' ? (
          <Campo etiqueta="Peso (kg)">
            <input type="number" step="0.1" min="20" max="400" value={pesoKg} required
                   onChange={(e) => setPesoKg(e.target.value)} />
          </Campo>
        ) : (
          <Campo etiqueta="Anos de servicio">
            <input type="number" min="0" max="80" value={aniosServicio} required
                   onChange={(e) => setAniosServicio(e.target.value)} />
          </Campo>
        )}
      </div>
      <button type="submit" disabled={enviando}>
        {enviando ? 'Creando...' : 'Crear usuario'}
      </button>
    </form>
  )
}

function FormularioEdicion({ usuario, onListo }: {
  usuario: UsuarioAdmin
  onListo: () => Promise<void>
}) {
  const [nombre, setNombre] = useState(usuario.nombre)
  const [apellido, setApellido] = useState(usuario.apellido)
  const [fechaNacimiento, setFechaNacimiento] = useState(usuario.fechaNacimiento)
  const [descripcion, setDescripcion] = useState(usuario.descripcion ?? '')
  const [pesoKg, setPesoKg] = useState(
    usuario.pesoKg === undefined ? '' : String(usuario.pesoKg))
  const [aniosServicio, setAniosServicio] = useState(
    usuario.aniosServicio === undefined ? '' : String(usuario.aniosServicio))
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await api.actualizar(`/api/admin/usuarios/${usuario.id}`, {
        nombre,
        apellido,
        fechaNacimiento,
        descripcion: descripcion.trim() === '' ? undefined : descripcion,
        pesoKg: usuario.rol === 'ATLETA' ? Number(pesoKg) : undefined,
        aniosServicio: usuario.rol === 'ENTRENADOR' ? Number(aniosServicio) : undefined,
      })
      await onListo()
    } catch (e) {
      setError(mensajeDe(e))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form onSubmit={enviar}>
      {error !== null && <Aviso tipo="error">{error}</Aviso>}
      <p className="ayuda">
        El email ({usuario.email}) y el rol no se pueden modificar.
      </p>
      <div className="fila">
        <Campo etiqueta="Nombre">
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} required />
        </Campo>
        <Campo etiqueta="Apellido">
          <input value={apellido} onChange={(e) => setApellido(e.target.value)} required />
        </Campo>
      </div>
      <div className="fila">
        <Campo etiqueta="Fecha de nacimiento">
          <input type="date" value={fechaNacimiento} required
                 onChange={(e) => setFechaNacimiento(e.target.value)} />
        </Campo>
        {usuario.rol === 'ATLETA' ? (
          <Campo etiqueta="Peso (kg)">
            <input type="number" step="0.1" min="20" max="400" value={pesoKg} required
                   onChange={(e) => setPesoKg(e.target.value)} />
          </Campo>
        ) : (
          <Campo etiqueta="Anos de servicio">
            <input type="number" min="0" max="80" value={aniosServicio} required
                   onChange={(e) => setAniosServicio(e.target.value)} />
          </Campo>
        )}
      </div>
      <Campo etiqueta="Descripcion">
        <textarea value={descripcion} onChange={(e) => setDescripcion(e.target.value)}
                  maxLength={1000} />
      </Campo>
      <button type="submit" disabled={enviando}>
        {enviando ? 'Guardando...' : 'Guardar cambios'}
      </button>
    </form>
  )
}
