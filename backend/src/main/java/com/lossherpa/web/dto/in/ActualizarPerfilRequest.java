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
 * RS06: lista blanca de lo editable del perfil (escritura a nivel de campo).
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
        Integer aniosServicio,

        // RF7 / RS06: opcional (null o vacio lo borra).
        @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
        @Pattern(regexp = "^\\s*(\\+?[0-9][0-9 ()\\-]{5,28}[0-9])?\\s*$",
                message = "El telefono solo puede tener numeros, espacios, parentesis y guiones")
        String telefono
) {
}
