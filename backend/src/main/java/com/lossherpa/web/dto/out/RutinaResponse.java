package com.lossherpa.web.dto.out;

import com.lossherpa.domain.EjercicioRutina;
import com.lossherpa.domain.OrigenRutina;
import com.lossherpa.domain.Rutina;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * RS07: DTO de salida explicito. Del entrenador creador se expone solo el nombre y
 * apellido, no su id ni su perfil.
 */
public record RutinaResponse(
        UUID id,
        String nombre,
        OrigenRutina origen,
        Instant creadaEn,
        String creadaPor,
        List<EjercicioResponse> ejercicios
) {

    public record EjercicioResponse(
            UUID id,
            int orden,
            String nombre,
            int series,
            int repeticiones
    ) {

        static EjercicioResponse de(EjercicioRutina ejercicio) {
            return new EjercicioResponse(ejercicio.getId(), ejercicio.getOrden(),
                    ejercicio.getNombre(), ejercicio.getSeries(), ejercicio.getRepeticiones());
        }
    }

    public static RutinaResponse de(Rutina rutina) {
        String creadaPor = rutina.getEntrenadorCreador() == null
                ? null
                : rutina.getEntrenadorCreador().getNombre() + " "
                        + rutina.getEntrenadorCreador().getApellido();

        return new RutinaResponse(
                rutina.getId(),
                rutina.getNombre(),
                rutina.getOrigen(),
                rutina.getCreadaEn(),
                creadaPor,
                rutina.getEjercicios().stream().map(EjercicioResponse::de).toList());
    }
}
