package com.lossherpa.web.dto.in;

import com.lossherpa.domain.Tema;
import jakarta.validation.constraints.NotNull;

/**
 * RF7: lista blanca de las preferencias operativas editables.
 *
 * Solo el tema. Email, rol y telefono no estan aca: como Jackson rechaza propiedades
 * desconocidas, un body con alguno de esos campos devuelve 400. Un tema fuera del enum
 * tambien termina en 400 (JSON ilegible).
 */
public record ActualizarPreferenciasRequest(
        @NotNull(message = "El tema es obligatorio")
        Tema tema
) {
}
