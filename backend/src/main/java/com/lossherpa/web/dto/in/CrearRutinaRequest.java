package com.lossherpa.web.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Lista blanca para crear una rutina. Sirve para las dos variantes:
 *  - el atleta crea su rutina PROPIA (el dueno es el usuario autenticado),
 *  - el entrenador crea una rutina ASIGNADA (el dueno sale del path, previo vinculo activo).
 *
 * No incluye origen, ni dueno, ni entrenador creador: esos los decide el servicio segun
 * quien este autenticado, nunca el cliente.
 */
public record CrearRutinaRequest(

        @NotBlank(message = "El nombre de la rutina es obligatorio")
        @Size(max = 120, message = "El nombre de la rutina es demasiado largo")
        String nombre,

        @NotEmpty(message = "La rutina tiene que tener al menos un ejercicio")
        @Size(max = 30, message = "Una rutina no puede tener mas de 30 ejercicios")
        @Valid
        List<EjercicioRequest> ejercicios
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
            int repeticiones
    ) {
    }
}
