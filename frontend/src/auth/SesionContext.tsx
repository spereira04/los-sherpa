import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { api, registrarManejadorDeSesionPerdida, type Rol } from '../api/cliente'
import type { Sesion } from '../api/tipos'

type EstadoSesion = 'cargando' | 'lista'

type ContextoSesion = {
  estado: EstadoSesion
  sesion: Sesion | null
  entrar: (email: string, contrasena: string) => Promise<Sesion>
  salir: () => Promise<void>
  /** Vuelve a preguntarle al backend quien es el usuario. */
  refrescar: () => Promise<void>
}

const Contexto = createContext<ContextoSesion | null>(null)

/** Panel inicial de cada rol. El backend igual valida todo, esto es solo navegacion. */
export function panelDe(rol: Rol): string {
  switch (rol) {
    case 'ENTRENADOR':
      return '/entrenador/atletas'
    case 'ATLETA':
      return '/atleta/rutinas'
    case 'ADMIN':
      return '/admin/usuarios'
  }
}

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoSesion>('cargando')
  const [sesion, setSesion] = useState<Sesion | null>(null)

  const cargarSesionActual = useCallback(async () => {
    try {
      const actual = await api.obtener<Sesion>('/api/auth/yo')
      setSesion(actual)
    } catch {
      // 401 al arrancar es lo normal cuando no hay sesion abierta.
      setSesion(null)
    } finally {
      setEstado('lista')
    }
  }, [])

  useEffect(() => {
    // RS10: si el backend invalida la sesion, el cliente la olvida en el acto.
    registrarManejadorDeSesionPerdida(() => setSesion(null))

    void (async () => {
      try {
        await api.prepararCsrf()
      } catch {
        // Sin token CSRF igual se muestra la pantalla de login y se reintenta al enviar.
      }
      await cargarSesionActual()
    })()
  }, [cargarSesionActual])

  const entrar = useCallback(async (email: string, contrasena: string) => {
    const nueva = await api.crear<Sesion>('/api/auth/login', { email, contrasena })
    setSesion(nueva)
    return nueva
  }, [])

  const salir = useCallback(async () => {
    try {
      await api.crear<void>('/api/auth/logout')
    } finally {
      setSesion(null)
    }
  }, [])

  const valor = useMemo<ContextoSesion>(
    () => ({ estado, sesion, entrar, salir, refrescar: cargarSesionActual }),
    [estado, sesion, entrar, salir, cargarSesionActual],
  )

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useSesion(): ContextoSesion {
  const contexto = useContext(Contexto)
  if (contexto === null) {
    throw new Error('useSesion tiene que usarse dentro de ProveedorSesion')
  }
  return contexto
}
