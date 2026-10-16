package com.lossherpa.security;

import org.springframework.security.access.AccessDeniedException;

/**
 * RS01 - Denegacion decidida por la politica de acceso.
 *
 * Lleva el motivo real para el log de auditoria (RS12). El motivo nunca llega al cliente:
 * la respuesta siempre es el 403 generico de ManejadorAccesoDenegado o del handler global.
 */
public class AccesoDenegadoPorPolitica extends AccessDeniedException {

    private final String motivo;

    public AccesoDenegadoPorPolitica(String motivo) {
        super("Acceso denegado por la politica");
        this.motivo = motivo;
    }

    public String getMotivo() {
        return motivo;
    }
}
