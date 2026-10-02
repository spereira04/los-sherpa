package com.lossherpa.web.dto.out;

/**
 * Token CSRF para el SPA. Se devuelve tambien en el body, ademas de la cookie XSRF-TOKEN,
 * para que el cliente pueda armar la cabecera sin depender de leer la cookie.
 */
public record CsrfResponse(String token, String nombreCabecera) {
}
