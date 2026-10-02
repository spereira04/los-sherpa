package com.lossherpa.error;

/** El estado actual del recurso impide la operacion. Se traduce a 409. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
