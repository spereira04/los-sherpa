// RF7: el tema se aplica con un atributo en <html> (la CSP es style-src 'self', sin inline).
// La fuente de verdad es el perfil; localStorage solo evita el parpadeo antes de que
// responda /api/auth/yo.
export type Tema = 'CLARO' | 'OSCURO' | 'SISTEMA'

const CLAVE = 'tema'

function esTema(valor: unknown): valor is Tema {
  return valor === 'CLARO' || valor === 'OSCURO' || valor === 'SISTEMA'
}

export function aplicarTema(tema: Tema): void {
  document.documentElement.setAttribute('data-tema', tema.toLowerCase())
  try {
    localStorage.setItem(CLAVE, tema)
  } catch {
    // localStorage puede estar bloqueado: el perfil sigue siendo la fuente de verdad.
  }
}

/** Aplica el ultimo tema conocido, antes de renderizar. */
export function aplicarTemaGuardado(): void {
  let guardado: string | null = null
  try {
    guardado = localStorage.getItem(CLAVE)
  } catch {
    // ver arriba
  }
  document.documentElement.setAttribute(
    'data-tema',
    (esTema(guardado) ? guardado : 'SISTEMA').toLowerCase(),
  )
}
