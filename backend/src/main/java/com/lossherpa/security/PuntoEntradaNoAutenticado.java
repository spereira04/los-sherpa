package com.lossherpa.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 para cualquier request sin sesion a un recurso protegido.
 * RS12: deja constancia del acceso denegado. RS14: responde JSON generico, sin basic auth.
 */
@Component
public class PuntoEntradaNoAutenticado implements AuthenticationEntryPoint {

    private final AuditorAcceso auditor;

    public PuntoEntradaNoAutenticado(AuditorAcceso auditor) {
        this.auditor = auditor;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException excepcion) throws IOException {
        // RS12: usuario anonimo, sin volcar el mensaje interno de la excepcion.
        auditor.denegado(request, null, HttpStatus.UNAUTHORIZED.value(), "sin sesion autenticada");
        RespuestaErrorSeguridad.escribir(response, HttpStatus.UNAUTHORIZED.value(),
                "No autenticado");
    }
}
