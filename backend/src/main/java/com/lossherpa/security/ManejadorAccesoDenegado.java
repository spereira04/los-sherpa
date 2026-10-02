package com.lossherpa.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 para el usuario autenticado que intenta una accion fuera de su rol.
 * RS12: registra el intento con el id del usuario y el recurso.
 */
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private final AuditorAcceso auditor;

    public ManejadorAccesoDenegado(AuditorAcceso auditor) {
        this.auditor = auditor;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excepcion) throws IOException {
        auditor.denegado(request, UsuarioActual.idOpcional().map(Object::toString).orElse(null),
                HttpStatus.FORBIDDEN.value(), "accion fuera del rol o CSRF invalido");
        RespuestaErrorSeguridad.escribir(response, HttpStatus.FORBIDDEN.value(),
                "No tiene permisos para realizar esta accion");
    }
}
