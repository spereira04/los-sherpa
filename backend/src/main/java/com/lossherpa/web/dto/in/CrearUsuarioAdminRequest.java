package com.lossherpa.web.dto.in;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Alta de usuario desde el panel de admin.
 *
 * Usa el mismo tipo RolRegistro que el registro publico, que no incluye ADMIN: el admin
 * tampoco puede crear otros admins. El unico camino para un admin son las variables de
 * entorno al arrancar (CreadorAdminInicial).
 */
public record CrearUsuarioAdminRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 254, message = "El email es demasiado largo")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(max = 128, message = "La contrasena no puede superar los 128 caracteres")
        String contrasena,

        @NotNull(message = "Hay que elegir entrenador o atleta")
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

        @DecimalMin(value = "20.0", message = "El peso tiene que ser de al menos 20 kg")
        @DecimalMax(value = "400.0", message = "El peso no puede superar los 400 kg")
        Double pesoKg,

        @Min(value = 0, message = "Los anos de servicio no pueden ser negativos")
        @Max(value = 80, message = "Los anos de servicio no pueden superar 80")
        Integer aniosServicio
) {

    /** Reusa el registro publico para no duplicar las reglas de alta. */
    public RegistroRequest aRegistro() {
        return new RegistroRequest(email, contrasena, rol, nombre, apellido, fechaNacimiento,
                descripcion, pesoKg, aniosServicio);
    }
}
