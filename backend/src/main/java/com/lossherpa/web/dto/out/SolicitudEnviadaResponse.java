package com.lossherpa.web.dto.out;

import com.lossherpa.domain.EstadoSolicitud;
import com.lossherpa.domain.SolicitudVinculacion;

import java.time.Instant;
import java.util.UUID;

/** Solicitud tal como la ve el atleta que la envio. */
public record SolicitudEnviadaResponse(
        UUID id,
        EstadoSolicitud estado,
        Instant creadaEn,
        PerfilPublicoResponse entrenador
) {

    public static SolicitudEnviadaResponse de(SolicitudVinculacion solicitud) {
        return new SolicitudEnviadaResponse(
                solicitud.getId(),
                solicitud.getEstado(),
                solicitud.getCreadaEn(),
                PerfilPublicoResponse.deEntrenador(solicitud.getEntrenador()));
    }
}
