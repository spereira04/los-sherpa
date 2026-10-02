import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import type { Rol } from '../api/cliente'
import { useSesion } from '../auth/SesionContext'

const MENUS: Record<Rol, { a: string; texto: string }[]> = {
  ENTRENADOR: [
    { a: '/entrenador/atletas', texto: 'Mis atletas' },
    { a: '/entrenador/perfil', texto: 'Mi perfil' },
  ],
  ATLETA: [
    { a: '/atleta/rutinas', texto: 'Mis rutinas' },
    { a: '/atleta/historial', texto: 'Historial' },
    { a: '/atleta/entrenador', texto: 'Mi entrenador' },
    { a: '/atleta/perfil', texto: 'Mi perfil' },
  ],
  ADMIN: [{ a: '/admin/usuarios', texto: 'Usuarios' }],
}

/**
 * El menu muestra solo lo del rol de la sesion. Es presentacion: la autorizacion real la
 * aplica el backend en cada request.
 */
export function Layout() {
  const { sesion, salir } = useSesion()
  const navegar = useNavigate()

  async function cerrarSesion() {
    await salir()
    navegar('/login', { replace: true })
  }

  return (
    <>
      <header className="encabezado">
        <span className="marca">Los Sherpa</span>
        {sesion !== null && (
          <nav>
            {MENUS[sesion.rol].map((item) => (
              <NavLink
                key={item.a}
                to={item.a}
                className={({ isActive }) => (isActive ? 'activo' : undefined)}
              >
                {item.texto}
              </NavLink>
            ))}
          </nav>
        )}
        {sesion !== null && (
          <div className="usuario">
            <span>
              {sesion.nombre} {sesion.apellido}
            </span>
            <button type="button" className="secundario" onClick={() => void cerrarSesion()}>
              Salir
            </button>
          </div>
        )}
      </header>
      <main className="contenido">
        <Outlet />
      </main>
    </>
  )
}
