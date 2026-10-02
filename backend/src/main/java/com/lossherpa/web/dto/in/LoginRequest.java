package com.lossherpa.web.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lista blanca del login: solo email y contrasena. */
public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        @Size(max = 254, message = "El email es demasiado largo")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(max = 128, message = "La contrasena no puede superar los 128 caracteres")
        String contrasena
) {
}
