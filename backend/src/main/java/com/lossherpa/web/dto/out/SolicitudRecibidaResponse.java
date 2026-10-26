package com.lossherpa.web.dto.out;

import com.lossherpa.domain.EstadoSolicitud;
import com.lossherpa.domain.SolicitudVinculacion;
import com.lossherpa.service.CalculadoraEdad;

import java.time.Instant;
import java.util.UUID;

/**
 * Solicitud tal como la ve el entrenador que la recibio.
 *
 * RS06: todavia no existe el vinculo, asi que el entrenador ve del solicitante solo lo que
 * necesita para decidir si acepta: nombre, apellido y edad. No lleva el id del atleta (se
 * acepta o rechaza con el id de la solicitud), ni su peso ni su descripcion: esos datos
 * recien se muestran cuando el vinculo esta vigente (PerfilPublicoResponse.deAtleta).
 */
public record SolicitudRecibidaResponse(
        UUID id,
        EstadoSolicitud estado,
        Instant creadaEn,
        Solicitante atleta
) {
    public record Solicitante(String nombre, String apellido, int edad) {
    }

    public static SolicitudRecibidaResponse de(SolicitudVinculacion solicitud) {
        return new SolicitudRecibidaResponse(
                solicitud.getId(),
                solicitud.getEstado(),
                solicitud.getCreadaEn(),
                new Solicitante(
                        solicitud.getAtleta().getNombre(),
                        solicitud.getAtleta().getApellido(),
                        CalculadoraEdad.edad(solicitud.getAtleta().getFechaNacimiento())));
    }
}
