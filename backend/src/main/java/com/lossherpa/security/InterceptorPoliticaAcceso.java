package com.lossherpa.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * RS01 - Segundo punto de aplicacion de la politica (defensa en profundidad).
 *
 * El filtro de seguridad decide sobre la URL de la request. Este interceptor vuelve a
 * decidir, con la misma PoliticaAcceso, sobre el patron del handler que MVC eligio de
 * verdad. Si alguien abre o borra por error una regla en SecurityConfig, o si la URL y el
 * handler no coinciden (normalizacion de paths, un handler agregado sin regla), la request
 * igual se deniega antes de llegar al controlador.
 *
 * Fail closed: un handler sin patron conocido o sin regla se deniega.
 */
@Component
public class InterceptorPoliticaAcceso implements HandlerInterceptor {

    private final PoliticaAcceso politica;

    public InterceptorPoliticaAcceso(PoliticaAcceso politica) {
        this.politica = politica;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        Object patron = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (!(patron instanceof String patronDelHandler)) {
            throw new AccesoDenegadoPorPolitica("handler sin patron declarado");
        }

        PoliticaAcceso.Regla regla = politica
                .buscar(HttpMethod.valueOf(request.getMethod()), patronDelHandler)
                .orElseThrow(() -> new AccesoDenegadoPorPolitica(
                        "handler sin regla en la politica: " + request.getMethod() + " "
                                + patronDelHandler));

        politica.exigir(regla, InterceptorPoliticaAcceso::autenticacionActual);
        return true;
    }

    private static Authentication autenticacionActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            throw new AuthenticationCredentialsNotFoundException("Sin sesion");
        }
        return autenticacion;
    }
}
