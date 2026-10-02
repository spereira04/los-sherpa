package com.lossherpa.web.dto.out;

import com.lossherpa.domain.Vinculo;

import java.time.LocalDate;

/**
 * Historial de exatletas del entrenador.
 *
 * RS07 + reglas de visibilidad: a proposito NO lleva el id del atleta, ni su edad, ni su
 * peso, ni su descripcion. Solo nombre, apellido y las fechas del vinculo, que es
 * exactamente lo que el enunciado permite conservar. Sin id, el entrenador no tiene con que
 * intentar pedir otros recursos de ese exatleta.
 */
public record ExatletaResponse(
        String nombre,
        String apellido,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {

    public static ExatletaResponse de(Vinculo vinculo) {
        return new ExatletaResponse(
                vinculo.getAtleta().getNombre(),
                vinculo.getAtleta().getApellido(),
                vinculo.getFechaInicio(),
                vinculo.getFechaFin());
    }
}
