package com.lossherpa.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * RS02 - Deny by default para requests no contempladas.
 *
 * Es la ultima regla de la cadena: atiende toda request que no coincidio con ninguna regla
 * de PoliticaAcceso (path inexistente, metodo no declarado, HEAD, OPTIONS fuera de un
 * preflight CORS, TRACE). Nunca concede: siempre termina en una denegacion auditada.
 *
 *  - Dentro de una zona de la API que exige sesion y sin sesion: 401. Primero hay que
 *    autenticarse; no se le dice a un anonimo nada sobre que existe y que no.
 *  - En cualquier otro caso: 403 con motivo "request no contemplada". La ruta no existe
 *    para nadie, asi que no hay recurso que enumerar (mismo criterio que RS13).
 */
@Component
public class DenegacionPorDefecto implements AuthorizationManager<RequestAuthorizationContext> {

    /** Zonas de la API donde toda operacion exige sesion. */
    private static final RequestMatcher ZONAS_CON_SESION = new OrRequestMatcher(
            AntPathRequestMatcher.antMatcher("/api/atleta/**"),
            AntPathRequestMatcher.antMatcher("/api/entrenador/**"),
            AntPathRequestMatcher.antMatcher("/api/admin/**"),
            AntPathRequestMatcher.antMatcher("/api/perfil/**"),
            AntPathRequestMatcher.antMatcher("/api/auth/**"));

    static final String MOTIVO = "request no contemplada";

    @Override
    public AuthorizationDecision check(Supplier<Authentication> autenticacion,
                                       RequestAuthorizationContext contexto) {
        if (ZONAS_CON_SESION.matches(contexto.getRequest())) {
            // Sin sesion, el proveedor lanza AuthenticationCredentialsNotFoundException: 401.
            autenticacion.get();
        }
        throw new AccesoDenegadoPorPolitica(MOTIVO);
    }
}
