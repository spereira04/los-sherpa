package com.lossherpa.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 para el usuario autenticado que intenta una accion fuera de su rol, para una request
 * sin regla en la politica y para un CSRF invalido.
 * RS12: registra el intento con el id del usuario, el recurso y el motivo que dio la politica.
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
                HttpStatus.FORBIDDEN.value(), motivoDe(excepcion));
        RespuestaErrorSeguridad.escribir(response, HttpStatus.FORBIDDEN.value(),
                "No tiene permisos para realizar esta accion");
    }

    /** RS12: el motivo real queda en el log; al cliente le llega siempre el mismo 403. */
    static String motivoDe(AccessDeniedException excepcion) {
        if (excepcion instanceof AccesoDenegadoPorPolitica denegacion) {
            return denegacion.getMotivo();
        }
        if (excepcion instanceof CsrfException) {
            return "token CSRF invalido o ausente";
        }
        return "accion fuera del rol o CSRF invalido";
    }
}
