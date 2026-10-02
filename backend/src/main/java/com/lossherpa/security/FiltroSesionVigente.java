package com.lossherpa.security;

import com.lossherpa.domain.Usuario;
import com.lossherpa.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

        Optional<Usuario> vigente = usuarios.findById(principal.getId());
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
        UsuarioAutenticado refrescado = new UsuarioAutenticado(vigente.get());
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

    private void invalidarSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
    }
}
