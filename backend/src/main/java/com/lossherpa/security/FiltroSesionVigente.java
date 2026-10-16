package com.lossherpa.security;

import com.lossherpa.domain.Usuario;
import com.lossherpa.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * RS10 - Impacto inmediato de los cambios de autorizacion (ASVS v5 8.3.2).
 *
 * En cada request con sesion, el principal se vuelve a cargar desde la base de datos antes
 * de que corran las reglas de autorizacion. Consecuencias:
 *
 *  - Si el admin elimino al usuario, la sesion se invalida en el request siguiente (401).
 *  - Los authorities se reconstruyen desde la fila actual, asi que no queda ninguna cache
 *    de permisos viva dentro de la sesion HTTP.
 *
 * La perdida de acceso por desvinculacion no necesita nada de esto: las consultas de
 * rutinas y ejecuciones exigen el vinculo activo en el propio where (ver RutinaRepository).
 */

public class FiltroSesionVigente extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(FiltroSesionVigente.class);

    private final UsuarioRepository usuarios;
    private final AuditorAcceso auditor;

    public FiltroSesionVigente(UsuarioRepository usuarios, AuditorAcceso auditor) {
        this.usuarios = usuarios;
        this.auditor = auditor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof UsuarioAutenticado principal)) {
            chain.doFilter(request, response);
            return;
        }

        // RS18 - Fail closed: si no se puede confirmar quien es el usuario (base caida, SQLite
        // bloqueado, fila inconsistente) la request NO sigue con el principal que quedo en la
        // sesion. Ese principal puede tener un rol viejo o un usuario ya eliminado, y dejarlo
        // pasar seria fail open: al atacante le alcanzaria con provocar el error.
        // El try cubre solo la recarga: lo que lance chain.doFilter mas abajo no es un fallo
        // de este filtro y no debe tragarse aca.
        Optional<Usuario> vigente;
        UsuarioAutenticado refrescado;
        try {
            vigente = usuarios.findById(principal.getId());
            refrescado = vigente.map(UsuarioAutenticado::new).orElse(null);
        } catch (RuntimeException e) {
            cortarPorFallo(request, response, principal, e);
            return;
        }

        if (vigente.isEmpty()) {
            // RS10: el usuario fue eliminado -> la sesion deja de valer ya en este request.
            invalidarSesion(request);
            SecurityContextHolder.clearContext();
            auditor.sesionInvalidada(request, principal.getId().toString(),
                    "el usuario de la sesion ya no existe");
            RespuestaErrorSeguridad.escribir(response, HttpStatus.UNAUTHORIZED.value(),
                    "La sesion ya no es valida");
            return;
        }

        // RS10: authorities recalculados contra la fila actual, no contra lo guardado en sesion.
        UsernamePasswordAuthenticationToken nuevoToken = new UsernamePasswordAuthenticationToken(
                refrescado, null, refrescado.getAuthorities());
        nuevoToken.setDetails(auth.getDetails());

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(nuevoToken);
        SecurityContextHolder.setContext(contexto);

        // No se limpia el contexto aca a proposito. El SecurityContextHolderFilter ya lo
        // limpia al terminar el request, y si lo limpiaramos nosotros el contexto quedaria
        // vacio cuando la AccessDeniedException sube hasta el ExceptionTranslationFilter:
        // el log de RS12 registraria "anonimo" en vez del id del usuario denegado.
        chain.doFilter(request, response);
    }

    /**
     * RS18: 503 generico, sin detalle. A diferencia del usuario eliminado (RS10) la sesion NO se
     * invalida: la falla es del servidor y puede ser transitoria, asi que cuando la base vuelva
     * el mismo usuario sigue logueado. Lo que si se descarta es el contexto de seguridad, para
     * que nada mas en esta request pueda usar el principal viejo.
     */
    private void cortarPorFallo(HttpServletRequest request, HttpServletResponse response,
                                UsuarioAutenticado principal, RuntimeException causa)
            throws IOException {
        LOG.error("No se pudo recargar el usuario de la sesion; se corta la request", causa);
        SecurityContextHolder.clearContext();
        auditor.falloCerrado(request, principal.getId().toString(),
                HttpStatus.SERVICE_UNAVAILABLE.value(), "FiltroSesionVigente", causa);
        RespuestaErrorSeguridad.escribir(response, HttpStatus.SERVICE_UNAVAILABLE.value(),
                "El servicio no esta disponible en este momento");
    }

    private void invalidarSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
    }
}
