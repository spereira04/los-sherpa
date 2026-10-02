package com.lossherpa.security;

/**
 * RS17 - Proteccion de logs.
 * Neutraliza la inyeccion en logs: elimina saltos de linea y caracteres de control de
 * cualquier dato que venga del usuario, escapa las comillas para que no se pueda romper
 * la estructura JSON del evento y acota el largo.
 */
public final class SanitizadorLog {

    private static final int LARGO_MAXIMO = 200;
    private static final String AUSENTE = "-";

    private SanitizadorLog() {
    }

    public static String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return AUSENTE;
        }
        String recortado = valor.length() > LARGO_MAXIMO
                ? valor.substring(0, LARGO_MAXIMO) + "...[truncado]"
                : valor;

        StringBuilder salida = new StringBuilder(recortado.length() + 8);
        for (int i = 0; i < recortado.length(); i++) {
            char c = recortado.charAt(i);
            // RS17: \r y \n permitirian inyectar una entrada de log falsa.
            if (c == '\r' || c == '\n' || c == '\t') {
                salida.append(' ');
            } else if (c == '"') {
                salida.append("\\\"");
            } else if (c == '\\') {
                salida.append("\\\\");
            } else if (Character.isISOControl(c)) {
                // Se descartan el resto de los caracteres de control (incluidos los ANSI).
                salida.append(' ');
            } else {
                salida.append(c);
            }
        }
        return salida.toString();
    }
}
