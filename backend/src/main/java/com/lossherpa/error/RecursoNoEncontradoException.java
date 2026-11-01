package com.lossherpa.error;

/**
 * El recurso no existe o no pertenece al usuario autenticado. Se traduce a 404.
 *
 * RS04/RS07: se usa el mismo 404 para los dos casos a proposito. Un 403 confirmaria que
 * el recurso existe y permitiria enumerar ids ajenos. El intento queda igual en el log
 * de autorizacion (RS12).
 *
 * RS12: el motivo real (inexistente, ajeno, archivado, ...) viaja aparte del mensaje y solo
 * llega al log de auditoria. El cliente recibe siempre el mismo mensaje, asi que distinguir
 * los casos en el log no reabre la enumeracion.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    private static final String MENSAJE_GENERICO = "El recurso solicitado no existe";

    private final String motivo;

    public RecursoNoEncontradoException(String mensaje, String motivo) {
        super(mensaje);
        this.motivo = motivo;
    }

    /** Mismo 404 generico para el cliente, con el motivo real para la auditoria. */
    public static RecursoNoEncontradoException conMotivo(String motivo) {
        return new RecursoNoEncontradoException(MENSAJE_GENERICO, motivo);
    }

    /**
     * El recurso no aparecio en la consulta acotada al usuario. {@code existe} dice si el id
     * existe en la tabla, para separar el id inventado del intento sobre un recurso ajeno.
     */
    public static RecursoNoEncontradoException segun(boolean existe, String recurso) {
        return conMotivo(existe ? recurso + " de otro usuario" : recurso + " inexistente");
    }

    public String getMotivo() {
        return motivo;
    }
}
