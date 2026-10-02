package com.lossherpa.error;

import com.lossherpa.security.AuditorAcceso;
import com.lossherpa.security.SanitizadorLog;
import com.lossherpa.security.UsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * RS14 - Sin configuracion por defecto: unico punto de salida de errores.
 * Las respuestas llevan un mensaje generico en espanol y nunca stack traces, nombres de
 * clase, SQL ni mensajes internos. El detalle tecnico queda solo en el log del servidor.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    private final AuditorAcceso auditor;

    public ManejadorGlobalErrores(AuditorAcceso auditor) {
        this.auditor = auditor;
    }

    /** Errores de Bean Validation: se devuelve que campo falla, nunca el valor recibido. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new TreeMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.put(error.getField(),
                    error.getDefaultMessage() == null ? "Valor invalido"
                            : error.getDefaultMessage());
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", "Hay datos invalidos en el formulario");
        cuerpo.put("campos", campos);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> reglaNegocio(ReglaNegocioException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Login fallido. Mensaje unico para email inexistente y contrasena incorrecta. */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> credencialesInvalidas(
            CredencialesInvalidasException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Violacion de una restriccion de la base (por ejemplo dos registros simultaneos con el
     * mismo email). RS14: nunca sale el nombre de la constraint ni el SQL.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException ex) {
        LOG.warn("Violacion de integridad en la base de datos", ex);
        return respuesta(HttpStatus.CONFLICT,
                "La operacion choca con un dato que ya existe");
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** RS04: cubre tanto "no existe" como "no es tuyo". Se registra como acceso denegado. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex,
                                                            HttpServletRequest request) {
        // RS12: el intento sobre un recurso ajeno o inexistente deja rastro.
        auditor.denegado(request, UsuarioActual.idOpcional().map(Object::toString).orElse(null),
                HttpStatus.NOT_FOUND.value(), "recurso inexistente o ajeno al usuario");
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accesoDenegado(HttpServletRequest request) {
        auditor.denegado(request, UsuarioActual.idOpcional().map(Object::toString).orElse(null),
                HttpStatus.FORBIDDEN.value(), "acceso denegado en la capa de servicio");
        return respuesta(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta accion");
    }

    /** Cuerpo JSON ilegible, parametro faltante o UUID mal formado: siempre 400 generico. */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> entradaInvalida(Exception ex) {
        LOG.debug("Entrada invalida: {}", SanitizadorLog.limpiar(ex.getClass().getSimpleName()));
        return respuesta(HttpStatus.BAD_REQUEST, "La solicitud no es valida");
    }

    /**
     * Path sin handler o estatico inexistente.
     *
     * NoResourceFoundException entra aca a proposito: sin este handler terminaba en el
     * catch-all de Exception y devolvia 500. Un estatico que no existe (o un directorio,
     * que el resolver rechaza) tiene que ser 404, no un error interno.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> sinHandler() {
        return respuesta(HttpStatus.NOT_FOUND, "El recurso solicitado no existe");
    }

    /**
     * Ultima red: cualquier excepcion no prevista se loguea del lado del servidor y al
     * cliente le llega un 500 sin detalle. RS14: nunca sale un stack trace por HTTP.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex) {
        LOG.error("Error inesperado procesando el request", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado");
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", mensaje == null ? "Ocurrio un error" : mensaje);
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
