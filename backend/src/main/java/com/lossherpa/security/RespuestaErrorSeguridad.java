package com.lossherpa.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * RS14: las respuestas de error de seguridad son un JSON minimo y generico.
 * Nunca incluyen stack traces, nombres de clase ni detalles internos.
 */
public final class RespuestaErrorSeguridad {

    private RespuestaErrorSeguridad() {
    }

    public static void escribir(HttpServletResponse response, int estado, String mensaje)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(estado);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
