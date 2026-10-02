import { Navigate, Route, Routes } from 'react-router-dom'
import { panelDe, useSesion } from './auth/SesionContext'
import { Layout } from './components/Layout'
import { RutaProtegida } from './components/RutaProtegida'
import { Cargando } from './components/ui'
import { MiPerfil } from './pages/MiPerfil'
import { Usuarios } from './pages/admin/Usuarios'
import { MiEntrenador } from './pages/atleta/MiEntrenador'
import { MiHistorial } from './pages/atleta/MiHistorial'
import { MisRutinas } from './pages/atleta/MisRutinas'
import { DetalleAtleta } from './pages/entrenador/DetalleAtleta'
import { MisAtletas } from './pages/entrenador/MisAtletas'
import { Login } from './pages/publicas/Login'
import { Registro } from './pages/publicas/Registro'

/** La raiz manda a cada rol a su panel, o al login si no hay sesion. */
function Inicio() {
  const { estado, sesion } = useSesion()
  if (estado === 'cargando') {
    return <Cargando />
  }
  return <Navigate to={sesion === null ? '/login' : panelDe(sesion.rol)} replace />
}

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Registro />} />

      <Route element={<Layout />}>
        <Route
          path="/entrenador/atletas"
          element={<RutaProtegida rol="ENTRENADOR"><MisAtletas /></RutaProtegida>}
        />
        <Route
          path="/entrenador/atletas/:idAtleta"
          element={<RutaProtegida rol="ENTRENADOR"><DetalleAtleta /></RutaProtegida>}
        />
        <Route
          path="/entrenador/perfil"
          element={<RutaProtegida rol="ENTRENADOR"><MiPerfil /></RutaProtegida>}
        />

        <Route
          path="/atleta/rutinas"
          element={<RutaProtegida rol="ATLETA"><MisRutinas /></RutaProtegida>}
        />
        <Route
          path="/atleta/historial"
          element={<RutaProtegida rol="ATLETA"><MiHistorial /></RutaProtegida>}
        />
        <Route
          path="/atleta/entrenador"
          element={<RutaProtegida rol="ATLETA"><MiEntrenador /></RutaProtegida>}
        />
        <Route
          path="/atleta/perfil"
          element={<RutaProtegida rol="ATLETA"><MiPerfil /></RutaProtegida>}
        />

        <Route
          path="/admin/usuarios"
          element={<RutaProtegida rol="ADMIN"><Usuarios /></RutaProtegida>}
        />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
