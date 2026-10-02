package com.lossherpa.web.dto.out;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;
import com.lossherpa.service.CalculadoraEdad;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Usuario visto por el admin. Es el unico DTO que expone el email de otra persona, porque
 * el admin gestiona cuentas. RS07: sigue sin exponer el hash de la contrasena.
 */
public record UsuarioAdminResponse(
        UUID id,
        String email,
        Rol rol,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        int edad,
        String descripcion,
        Double pesoKg,
        Integer aniosServicio,
        Instant creadoEn
) {

    public static UsuarioAdminResponse de(Usuario usuario) {
        return new UsuarioAdminResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getFechaNacimiento(),
                CalculadoraEdad.edad(usuario.getFechaNacimiento()),
                usuario.getDescripcion(),
                usuario.getPesoKg(),
                usuario.getAniosServicio(),
                usuario.getCreadoEn());
    }
}
