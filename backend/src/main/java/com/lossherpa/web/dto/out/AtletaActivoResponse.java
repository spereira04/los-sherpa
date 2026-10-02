package com.lossherpa.web.dto.out;

import com.lossherpa.domain.Vinculo;

import java.time.LocalDate;

/** Atleta con vinculo activo, como lo lista el entrenador. */
public record AtletaActivoResponse(PerfilPublicoResponse atleta, LocalDate desde) {

    public static AtletaActivoResponse de(Vinculo vinculo) {
        return new AtletaActivoResponse(
                PerfilPublicoResponse.deAtleta(vinculo.getAtleta()),
                vinculo.getFechaInicio());
    }
}
