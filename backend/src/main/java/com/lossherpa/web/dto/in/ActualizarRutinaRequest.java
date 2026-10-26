package com.lossherpa.web.dto.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * RS06: lista blanca para actualizar una rutina asignada por el entrenador.
 * Cualquier campo no declarado (origen, atleta, id, creador, fechas) se rechaza con 400:
 * lo hace Jackson en modo estricto, igual que en el resto de los DTOs de entrada.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record ActualizarRutinaRequest(

        @NotBlank(message = "El nombre de la rutina es obligatorio")
        @Size(max = 120, message = "El nombre de la rutina es demasiado largo")
        String nombre,

        @NotEmpty(message = "La rutina tiene que tener al menos un ejercicio")
        @Size(max = 30, message = "Una rutina no puede tener mas de 30 ejercicios")
        @Valid
        List<EjercicioRequest> ejercicios,

        @Size(max = 500, message = "Las observaciones de la rutina son demasiado largas")
        String observaciones
) {

    public record EjercicioRequest(

            @NotBlank(message = "El nombre del ejercicio es obligatorio")
            @Size(max = 120, message = "El nombre del ejercicio es demasiado largo")
            String nombre,

            @Min(value = 1, message = "Un ejercicio tiene que tener al menos una serie")
            @Max(value = 20, message = "Un ejercicio no puede tener mas de 20 series")
            int series,

            @Min(value = 1, message = "Las repeticiones tienen que ser al menos 1")
            @Max(value = 500, message = "Las repeticiones no pueden superar 500")
            int repeticiones,

            @Size(max = 500, message = "Las observaciones del ejercicio son demasiado largas")
            String observaciones
    ) {
    }
}
