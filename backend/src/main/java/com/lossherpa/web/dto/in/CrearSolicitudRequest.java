package com.lossherpa.web.dto.in;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Lista blanca: el atleta solo elige a que entrenador le manda la solicitud.
 *
 * El atleta de la solicitud NO viaja en el body: se toma del usuario autenticado (RS04),
 * asi que no hay forma de crear una solicitud en nombre de otro.
 */
public record CrearSolicitudRequest(
        @NotNull(message = "Hay que elegir un entrenador")
        UUID idEntrenador
) {
}
