package com.lossherpa.security;

import com.lossherpa.domain.Rol;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Acceso al usuario autenticado desde la capa de servicio.
 *
 * RS04: los servicios obtienen el id del usuario de aca y lo pasan como parametro a las
 * consultas. Nunca aceptan un id de usuario que venga del cliente.
 */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static UUID id() {
        return idOpcional().orElseThrow(
                () -> new IllegalStateException("No hay usuario autenticado en el contexto"));
    }

    public static Optional<UUID> idOpcional() {
        return principal().map(UsuarioAutenticado::getId);
    }

    public static Rol rol() {
        return principal().map(UsuarioAutenticado::getRol)
                .orElseThrow(() -> new IllegalStateException("No hay usuario autenticado"));
    }

    private static Optional<UsuarioAutenticado> principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            return Optional.empty();
        }
        return Optional.of(usuario);
    }
}
