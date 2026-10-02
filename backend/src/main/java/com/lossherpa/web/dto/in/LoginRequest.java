package com.lossherpa.web.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Lista blanca del login: solo email y contrasena.
 *
 * contrasena es char[] y no String: quien la use (AuthController) la borra de memoria
 * despues de autenticar, en vez de dejarla como String inmutable hasta que pase el GC.
 */
public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        @Size(max = 254, message = "El email es demasiado largo")
        String email,

        @NotEmpty(message = "La contrasena es obligatoria")
        @Size(max = 128, message = "La contrasena no puede superar los 128 caracteres")
        char[] contrasena
) {
}
