package com.lossherpa.web.dto.out;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;

import java.util.UUID;

/**
 * RS07: DTO de salida explicito. Lo minimo que el frontend necesita para decidir a que
 * panel mandar al usuario. Nunca se serializa la entidad Usuario (traeria el hash de la
 * contrasena y las relaciones).
 */
public record SesionResponse(UUID id, String email, Rol rol, String nombre, String apellido) {

    public static SesionResponse de(Usuario usuario) {
        return new SesionResponse(usuario.getId(), usuario.getEmail(), usuario.getRol(),
                usuario.getNombre(), usuario.getApellido());
    }
}
