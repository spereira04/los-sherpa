package com.lossherpa.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * RS12 - Logs de autorizacion.
 * Registra cada acceso denegado en una linea JSON estructurada con timestamp, id del
 * usuario (o "anonimo"), IP, metodo, recurso y motivo.
 *
 * RS17: todo dato de origen externo pasa por SanitizadorLog. Nunca se loguean
 * contrasenas, cookies, cabeceras de autorizacion, tokens ni cuerpos de request.
 */
@Component
public class AuditorAcceso {

    private static final Logger LOG = LoggerFactory.getLogger("AUDITORIA");

    public void denegado(HttpServletRequest request, String usuarioId, int estado, String motivo) {
        LOG.warn("{\"ts\":\"{}\",\"evento\":\"acceso_denegado\",\"estado\":{},"
                        + "\"usuarioId\":\"{}\",\"ip\":\"{}\",\"metodo\":\"{}\","
                        + "\"recurso\":\"{}\",\"motivo\":\"{}\"}",
                Instant.now(),
                estado,
                SanitizadorLog.limpiar(usuarioId == null ? "anonimo" : usuarioId),
                SanitizadorLog.limpiar(ipDe(request)),
                SanitizadorLog.limpiar(request == null ? null : request.getMethod()),
                SanitizadorLog.limpiar(recursoDe(request)),
                SanitizadorLog.limpiar(motivo));
    }

    /** Login fallido: nunca se registra la contrasena, solo el email intentado. */
    public void loginFallido(HttpServletRequest request, String emailIntentado, String motivo) {
        LOG.warn("{\"ts\":\"{}\",\"evento\":\"login_fallido\",\"estado\":401,"
                        + "\"email\":\"{}\",\"ip\":\"{}\",\"motivo\":\"{}\"}",
                Instant.now(),
                SanitizadorLog.limpiar(emailIntentado),
                SanitizadorLog.limpiar(ipDe(request)),
                SanitizadorLog.limpiar(motivo));
    }

    public void sesionInvalidada(HttpServletRequest request, String usuarioId, String motivo) {
        LOG.warn("{\"ts\":\"{}\",\"evento\":\"sesion_invalidada\",\"estado\":401,"
                        + "\"usuarioId\":\"{}\",\"ip\":\"{}\",\"motivo\":\"{}\"}",
                Instant.now(),
                SanitizadorLog.limpiar(usuarioId),
                SanitizadorLog.limpiar(ipDe(request)),
                SanitizadorLog.limpiar(motivo));
    }

    private String ipDe(HttpServletRequest request) {
        // No se confia en X-Forwarded-For: la app corre en local sin proxy reverso.
        return request == null ? null : request.getRemoteAddr();
    }

    private String recursoDe(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // Solo metodo y path: nunca el query string ni el body, que pueden traer datos sensibles.
        return request.getRequestURI();
    }
}
