package com.lossherpa.error;

/**
 * El recurso no existe o no pertenece al usuario autenticado. Se traduce a 404.
 *
 * RS04/RS07: se usa el mismo 404 para los dos casos a proposito. Un 403 confirmaria que
 * el recurso existe y permitiria enumerar ids ajenos. El intento queda igual en el log
 * de autorizacion (RS12).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException generico() {
        return new RecursoNoEncontradoException("El recurso solicitado no existe");
    }
}
