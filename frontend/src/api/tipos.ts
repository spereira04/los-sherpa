import type { Rol } from './cliente'

export type Sesion = {
  id: string
  email: string
  rol: Rol
  nombre: string
  apellido: string
}

export type Perfil = {
  id: string
  email: string
  rol: Rol
  nombre: string
  apellido: string
  fechaNacimiento: string
  edad: number
  descripcion?: string
  pesoKg?: number
  aniosServicio?: number
}

export type PerfilPublico = {
  id: string
  nombre: string
  apellido: string
  edad: number
  descripcion?: string
  aniosServicio?: number
  pesoKg?: number
}

export type EstadoSolicitud = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA' | 'CANCELADA'

export type SolicitudEnviada = {
  id: string
  estado: EstadoSolicitud
  creadaEn: string
  entrenador: PerfilPublico
}

export type SolicitudRecibida = {
  id: string
  estado: EstadoSolicitud
  creadaEn: string
  atleta: PerfilPublico
}

export type Vinculo = {
  id: string
  fechaInicio: string
  fechaFin?: string
}

export type Exatleta = {
  nombre: string
  apellido: string
  fechaInicio: string
  fechaFin: string
}

export type OrigenRutina = 'ASIGNADA' | 'PROPIA'

export type EjercicioRutina = {
  id: string
  orden: number
  nombre: string
  series: number
  repeticiones: number
}

export type Rutina = {
  id: string
  nombre: string
  origen: OrigenRutina
  creadaEn: string
  creadaPor?: string
  ejercicios: EjercicioRutina[]
}

export type SerieEjecutada = {
  idEjercicio: string
  nombreEjercicio: string
  nroSerie: number
  cargaKg: number
}

export type Ejecucion = {
  id: string
  fecha: string
  idRutina: string
  nombreRutina: string
  series: SerieEjecutada[]
}

export type AtletaActivo = {
  atleta: PerfilPublico
  desde: string
}

export type UsuarioAdmin = {
  id: string
  email: string
  rol: Rol
  nombre: string
  apellido: string
  fechaNacimiento: string
  edad: number
  descripcion?: string
  pesoKg?: number
  aniosServicio?: number
  creadoEn: string
}
