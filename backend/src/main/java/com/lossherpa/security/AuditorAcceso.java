package com.lossherpa.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

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

    /**
     * RS18 - Fail closed: un componente de la cadena de autorizacion fallo y la request se
     * corto en lugar de seguir. El motivo lleva el componente y solo el NOMBRE de la clase de
     * la excepcion: el mensaje puede traer SQL, rutas o datos de usuario, asi que no se loguea
     * (el detalle tecnico queda en el log de la aplicacion, no en la auditoria).
     */
    public void falloCerrado(HttpServletRequest request, String usuarioId, int estado,
                             String componente, Throwable causa) {
        LOG.warn("{\"ts\":\"{}\",\"evento\":\"fallo_cerrado\",\"estado\":{},"
                        + "\"usuarioId\":\"{}\",\"ip\":\"{}\",\"metodo\":\"{}\","
                        + "\"recurso\":\"{}\",\"motivo\":\"{}\"}",
                Instant.now(),
                estado,
                SanitizadorLog.limpiar(usuarioId == null ? "anonimo" : usuarioId),
                SanitizadorLog.limpiar(ipDe(request)),
                SanitizadorLog.limpiar(request == null ? null : request.getMethod()),
                SanitizadorLog.limpiar(recursoDe(request)),
                SanitizadorLog.limpiar(componente + ": "
                        + (causa == null ? "desconocida" : causa.getClass().getSimpleName())));
    }
    /** RF4 + RS12: el evento no incluye datos de la ejecución. */
    public void ejecucionEliminada(UUID atletaId, UUID ejecucionId) {
        LOG.info("{\"ts\":\"{}\",\"evento\":\"ejecucion_eliminada\","
                        + "\"usuarioId\":\"{}\",\"ejecucionId\":\"{}\"}",
                Instant.now(),
                SanitizadorLog.limpiar(atletaId.toString()),
                SanitizadorLog.limpiar(ejecucionId.toString()));
    }

    /** RF4 + RS12: registrar la limpieza del historial sin enumerar sus ejecuciones. */
    public void historicoEjecucionesEliminado(UUID atletaId) {
        LOG.info("{\"ts\":\"{}\",\"evento\":\"historico_ejecuciones_eliminado\","
                        + "\"usuarioId\":\"{}\"}",
                Instant.now(),
                SanitizadorLog.limpiar(atletaId.toString()));
    }

    private String ipDe(HttpServletRequest request) {
        // No se confia en X-Forwarded-For: la app corre en local sin proxy reverso.
        return request == null ? null : request.getRemoteAddr();
    }

    private String recursoDe(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // RS18: en el dispatch ERROR el contenedor reenvia a /error y getRequestURI() deja de ser
        // el recurso que fallo. El path original viene en el atributo del contenedor.
        if (request.getDispatcherType() == DispatcherType.ERROR
                && request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String original) {
            return original;
        }
        // Solo metodo y path: nunca el query string ni el body, que pueden traer datos sensibles.
        return request.getRequestURI();
    }
}
