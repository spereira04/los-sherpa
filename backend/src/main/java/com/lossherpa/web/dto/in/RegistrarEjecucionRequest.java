package com.lossherpa.web.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lista blanca para registrar una ejecucion: la fecha y la carga de cada serie.
 *
 * La rutina sale del path y el atleta del usuario autenticado, no del body.
 * RS04: los `idEjercicio` que manda el cliente se validan contra los ejercicios de ESA
 * rutina; nunca se usan para buscar un ejercicio en toda la tabla.
 */
public record RegistrarEjecucionRequest(

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate fecha,

        @NotEmpty(message = "Hay que cargar las series realizadas")
        @Size(max = 600, message = "Demasiadas series en una sola ejecucion")
        @Valid
        List<SerieRequest> series
) {

    public record SerieRequest(

            @NotNull(message = "Falta el ejercicio de la serie")
            UUID idEjercicio,

            @Min(value = 1, message = "El numero de serie empieza en 1")
            int nroSerie,

            @DecimalMin(value = "0.0", message = "La carga no puede ser negativa")
            @DecimalMax(value = "1000.0", message = "La carga no puede superar los 1000 kg")
            double cargaKg
    ) {
    }
}
