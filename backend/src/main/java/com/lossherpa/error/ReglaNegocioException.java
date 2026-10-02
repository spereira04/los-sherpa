package com.lossherpa.error;

/** Entrada valida en forma pero rechazada por una regla de negocio. Se traduce a 400. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
