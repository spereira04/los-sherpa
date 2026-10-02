package com.lossherpa.web.dto.in;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Lista blanca de campos del registro. No hay campo id, ni hash de contrasena, ni ninguna
 * otra columna de autorizacion: lo que no esta aca no puede entrar (anti mass assignment).
 * El rol se limita a ENTRENADOR o ATLETA por el tipo RolRegistro.
 *
 * La contrasena solo se valida de largo maximo aca; la politica completa esta en
 * PoliticaContrasena, para dar un mensaje util y no 400 generico.
 *
 * contrasena es char[] y no String: ServicioAutenticacion la borra de memoria apenas la usa
 * para hashearla, en vez de dejarla como String inmutable hasta que pase el GC.
 */
public record RegistroRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 254, message = "El email es demasiado largo")
        String email,

        @NotEmpty(message = "La contrasena es obligatoria")
        @Size(max = 128, message = "La contrasena no puede superar los 128 caracteres")
        char[] contrasena,

        @NotNull(message = "Hay que elegir si te registras como entrenador o como atleta")
        RolRegistro rol,

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

        /** Solo para atletas. Si lo manda un entrenador, se rechaza. */
        @DecimalMin(value = "20.0", message = "El peso tiene que ser de al menos 20 kg")
        @DecimalMax(value = "400.0", message = "El peso no puede superar los 400 kg")
        Double pesoKg,

        /** Solo para entrenadores. Si lo manda un atleta, se rechaza. */
        @Min(value = 0, message = "Los anos de servicio no pueden ser negativos")
        @Max(value = 80, message = "Los anos de servicio no pueden superar 80")
        Integer aniosServicio
) {
}
