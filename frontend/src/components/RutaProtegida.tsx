import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import type { Rol } from '../api/cliente'
import { panelDe, useSesion } from '../auth/SesionContext'
import { Cargando } from './ui'

/**
 * Oculta las pantallas que no corresponden al rol.
 *
 * Esto es SOLO experiencia de usuario: la autorizacion real la aplica el backend en la
 * cadena de filtros y de nuevo en cada consulta (RS04, RS13). Saltarse esta guarda a mano
 * no da acceso a ningun dato.
 */
export function RutaProtegida({ rol, children }: { rol: Rol; children: ReactNode }) {
  const { estado, sesion } = useSesion()
  const ubicacion = useLocation()

  if (estado === 'cargando') {
    return <Cargando />
  }
  if (sesion === null) {
    return <Navigate to="/login" replace state={{ volverA: ubicacion.pathname }} />
  }
  if (sesion.rol !== rol) {
    return <Navigate to={panelDe(sesion.rol)} replace />
  }
  return <>{children}</>
}
