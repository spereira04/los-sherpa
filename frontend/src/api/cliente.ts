/**
 * Cliente HTTP unico de la aplicacion.
 *
 * - Siempre manda la cookie de sesion (`credentials: 'include'`).
 * - Obtiene el token CSRF de GET /api/csrf y lo reenvia en la cabecera X-XSRF-TOKEN en
 *   toda mutacion. Si el token quedo viejo (403), lo renueva y reintenta una sola vez.
 * - No guarda datos del usuario en localStorage: la sesion vive en el servidor, asi que
 *   cualquier cambio de autorizacion se refleja en el request siguiente (RS10).
 */

export type Rol = 'ENTRENADOR' | 'ATLETA' | 'ADMIN'

export class ErrorApi extends Error {
  readonly estado: number
  readonly campos?: Record<string, string>

  constructor(estado: number, mensaje: string, campos?: Record<string, string>) {
    super(mensaje)
    this.estado = estado
    this.campos = campos
  }
}

type Metodo = 'GET' | 'POST' | 'PUT' | 'DELETE'

let tokenCsrf: string | null = null
let nombreCabeceraCsrf = 'X-XSRF-TOKEN'

/** Se registra una sola vez desde el contexto de sesion para reaccionar a un 401. */
let alPerderLaSesion: (() => void) | null = null

export function registrarManejadorDeSesionPerdida(manejador: () => void): void {
  alPerderLaSesion = manejador
}

async function renovarCsrf(): Promise<void> {
  const respuesta = await fetch('/api/csrf', { credentials: 'include' })
  if (!respuesta.ok) {
    throw new ErrorApi(respuesta.status, 'No se pudo iniciar la sesion segura')
  }
  const datos = (await respuesta.json()) as { token: string; nombreCabecera: string }
  tokenCsrf = datos.token
  nombreCabeceraCsrf = datos.nombreCabecera
}

async function cabeceras(metodo: Metodo, tieneCuerpo: boolean): Promise<HeadersInit> {
  const resultado: Record<string, string> = { Accept: 'application/json' }
  if (tieneCuerpo) {
    resultado['Content-Type'] = 'application/json'
  }
  if (metodo !== 'GET') {
    if (tokenCsrf === null) {
      await renovarCsrf()
    }
    resultado[nombreCabeceraCsrf] = tokenCsrf as string
  }
  return resultado
}

async function leerError(respuesta: Response): Promise<ErrorApi> {
  let mensaje = 'Ocurrio un error'
  let campos: Record<string, string> | undefined
  try {
    const cuerpo = await respuesta.json()
    if (typeof cuerpo?.error === 'string') {
      mensaje = cuerpo.error
    }
    if (cuerpo?.campos && typeof cuerpo.campos === 'object') {
      campos = cuerpo.campos as Record<string, string>
    }
  } catch {
    // Respuesta sin cuerpo JSON: queda el mensaje generico.
  }
  return new ErrorApi(respuesta.status, mensaje, campos)
}

async function ejecutar<T>(
  metodo: Metodo,
  ruta: string,
  cuerpo?: unknown,
  esReintento = false,
): Promise<T> {
  const tieneCuerpo = cuerpo !== undefined
  const respuesta = await fetch(ruta, {
    method: metodo,
    credentials: 'include',
    headers: await cabeceras(metodo, tieneCuerpo),
    body: tieneCuerpo ? JSON.stringify(cuerpo) : undefined,
  })

  // Token CSRF vencido o rotado: se renueva y se reintenta una sola vez.
  if (respuesta.status === 403 && metodo !== 'GET' && !esReintento) {
    await renovarCsrf()
    return ejecutar<T>(metodo, ruta, cuerpo, true)
  }

  if (respuesta.status === 401) {
    // RS10: la sesion dejo de valer (logout, usuario eliminado por el admin, timeout).
    tokenCsrf = null
    if (alPerderLaSesion !== null) {
      alPerderLaSesion()
    }
    throw await leerError(respuesta)
  }

  if (!respuesta.ok) {
    throw await leerError(respuesta)
  }

  if (respuesta.status === 204) {
    return undefined as T
  }
  return (await respuesta.json()) as T
}

export const api = {
  obtener: <T>(ruta: string) => ejecutar<T>('GET', ruta),
  crear: <T>(ruta: string, cuerpo?: unknown) => ejecutar<T>('POST', ruta, cuerpo ?? {}),
  actualizar: <T>(ruta: string, cuerpo: unknown) => ejecutar<T>('PUT', ruta, cuerpo),
  borrar: <T>(ruta: string) => ejecutar<T>('DELETE', ruta),
  /** Se llama al arrancar la app para tener la cookie XSRF-TOKEN desde el primer momento. */
  prepararCsrf: renovarCsrf,
}
