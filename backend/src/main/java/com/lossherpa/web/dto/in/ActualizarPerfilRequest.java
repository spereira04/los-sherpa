package com.lossherpa.web.dto.in;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Lista blanca de lo editable del perfil.
 *
 * No incluye email (inmutable), rol (inmutable), id, contrasena ni ningun campo de
 * autorizacion. Como ademas el mapper de Jackson rechaza propiedades desconocidas
 * (fail-on-unknown-properties = true), un body con "rol" o "id" devuelve 400 en lugar de
 * ignorarse en silencio.
 */
public record ActualizarPerfilRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre es demasiado largo")
        @Pattern(regexp = "^[\\p{L}\\p{M} '.\\-]+$",
                message = "El nombre solo puede tener letras, espacios, apostrofos y guiones")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 80, message = "El apellido es demasiado largo")
        @Pattern(regexp = "^[\\p{L}\\p{M} '.\\-]+$",
                message = "El apellido solo puede tener letras, espacios, apostrofos y guiones")
        String apellido,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento tiene que ser anterior a hoy")
        LocalDate fechaNacimiento,

        @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
        String descripcion,

        @DecimalMin(value = "20.0", message = "El peso tiene que ser de al menos 20 kg")
        @DecimalMax(value = "400.0", message = "El peso no puede superar los 400 kg")
        Double pesoKg,

        @Min(value = 0, message = "Los anos de servicio no pueden ser negativos")
        @Max(value = 80, message = "Los anos de servicio no pueden superar 80")
        Integer aniosServicio
) {
}
