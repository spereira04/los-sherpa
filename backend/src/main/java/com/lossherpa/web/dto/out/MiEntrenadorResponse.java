package com.lossherpa.web.dto.out;

import com.lossherpa.domain.Vinculo;

import java.time.LocalDate;

/** Entrenador activo del atleta, con la fecha de inicio del vinculo. */
public record MiEntrenadorResponse(PerfilPublicoResponse entrenador, LocalDate desde) {

    public static MiEntrenadorResponse de(Vinculo vinculo) {
        return new MiEntrenadorResponse(
                PerfilPublicoResponse.deEntrenador(vinculo.getEntrenador()),
                vinculo.getFechaInicio());
    }
}
