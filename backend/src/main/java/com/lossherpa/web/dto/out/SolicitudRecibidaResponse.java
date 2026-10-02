package com.lossherpa.web.dto.out;

import com.lossherpa.domain.EstadoSolicitud;
import com.lossherpa.domain.SolicitudVinculacion;

import java.time.Instant;
import java.util.UUID;

/** Solicitud tal como la ve el entrenador que la recibio. */
public record SolicitudRecibidaResponse(
        UUID id,
        EstadoSolicitud estado,
        Instant creadaEn,
        PerfilPublicoResponse atleta
) {

    public static SolicitudRecibidaResponse de(SolicitudVinculacion solicitud) {
        return new SolicitudRecibidaResponse(
                solicitud.getId(),
                solicitud.getEstado(),
                solicitud.getCreadaEn(),
                PerfilPublicoResponse.deAtleta(solicitud.getAtleta()));
    }
}
