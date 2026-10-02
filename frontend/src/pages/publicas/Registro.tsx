import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../api/cliente'
import { mensajeDe } from '../../api/useRecurso'
import { Aviso, Campo, Tarjeta } from '../../components/ui'

type RolElegible = 'ATLETA' | 'ENTRENADOR'

export function Registro() {
  const navegar = useNavigate()
  const [rol, setRol] = useState<RolElegible>('ATLETA')
  const [email, setEmail] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [nombre, setNombre] = useState('')
  const [apellido, setApellido] = useState('')
  const [fechaNacimiento, setFechaNacimiento] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [pesoKg, setPesoKg] = useState('')
  const [aniosServicio, setAniosServicio] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await api.crear('/api/auth/registro', {
        email,
        contrasena,
        rol,
        nombre,
        apellido,
        fechaNacimiento,
        descripcion: descripcion.trim() === '' ? undefined : descripcion,
        pesoKg: rol === 'ATLETA' ? Number(pesoKg) : undefined,
        aniosServicio: rol === 'ENTRENADOR' ? Number(aniosServicio) : undefined,
      })
      navegar('/login', { replace: true })
    } catch (e) {
      setError(mensajeDe(e))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="contenido-angosto">
      <h1>Crear cuenta</h1>
      <Tarjeta>
        {error !== null && <Aviso tipo="error">{error}</Aviso>}
        <form onSubmit={enviar}>
          <Campo etiqueta="Me registro como">
            <select value={rol} onChange={(e) => setRol(e.target.value as RolElegible)}>
              <option value="ATLETA">Atleta</option>
              <option value="ENTRENADOR">Entrenador</option>
            </select>
          </Campo>
          <p className="ayuda">El rol no se puede cambiar despues.</p>

          <Campo etiqueta="Email">
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                   autoComplete="username" required />
          </Campo>
          <Campo etiqueta="Contrasena">
            <input type="password" value={contrasena}
                   onChange={(e) => setContrasena(e.target.value)}
                   autoComplete="new-password" minLength={12} required />
          </Campo>
          <p className="ayuda">
            Minimo 12 caracteres. Se aceptan espacios y cualquier simbolo. No se admiten
            contrasenas muy usadas ni que contengan tu email, nombre o apellido.
          </p>

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

          {rol === 'ATLETA' ? (
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

          <Campo etiqueta="Descripcion (opcional)">
            <textarea value={descripcion} onChange={(e) => setDescripcion(e.target.value)}
                      maxLength={1000} />
          </Campo>

          <button type="submit" disabled={enviando}>
            {enviando ? 'Creando...' : 'Crear cuenta'}
          </button>
        </form>
        <p className="ayuda" style={{ marginTop: '1rem' }}>
          Ya tenes cuenta? <Link to="/login">Inicia sesion</Link>
        </p>
      </Tarjeta>
    </div>
  )
}
