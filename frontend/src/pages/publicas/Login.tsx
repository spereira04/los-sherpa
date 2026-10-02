import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { mensajeDe } from '../../api/useRecurso'
import { panelDe, useSesion } from '../../auth/SesionContext'
import { Aviso, Campo, Tarjeta } from '../../components/ui'

export function Login() {
  const { entrar } = useSesion()
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const [email, setEmail] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      const sesion = await entrar(email, contrasena)
      const volverA = (ubicacion.state as { volverA?: string } | null)?.volverA
      navegar(volverA ?? panelDe(sesion.rol), { replace: true })
    } catch (e) {
      setError(mensajeDe(e))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="contenido-angosto">
      <h1>Los Sherpa</h1>
      <Tarjeta titulo="Iniciar sesion">
        {error !== null && <Aviso tipo="error">{error}</Aviso>}
        <form onSubmit={enviar}>
          <Campo etiqueta="Email">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
              required
            />
          </Campo>
          <Campo etiqueta="Contrasena">
            <input
              type="password"
              value={contrasena}
              onChange={(e) => setContrasena(e.target.value)}
              autoComplete="current-password"
              required
            />
          </Campo>
          <button type="submit" disabled={enviando}>
            {enviando ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>
        <p className="ayuda" style={{ marginTop: '1rem' }}>
          No tenes cuenta? <Link to="/registro">Registrate</Link>
        </p>
      </Tarjeta>
    </div>
  )
}
