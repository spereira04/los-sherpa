package com.lossherpa.web.dto.out;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;
import com.lossherpa.service.CalculadoraEdad;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Perfil propio del usuario autenticado.
 *
 * RS07: DTO explicito, sin hash de contrasena ni ids internos.
 * La edad viaja calculada; la fecha de nacimiento tambien, porque es el campo editable.
 */
public record PerfilResponse(
        UUID id,
        String email,
        Rol rol,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        int edad,
        String descripcion,
        Double pesoKg,
        Integer aniosServicio
) {

    public static PerfilResponse de(Usuario usuario) {
        return new PerfilResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getFechaNacimiento(),
                CalculadoraEdad.edad(usuario.getFechaNacimiento()),
                usuario.getDescripcion(),
                usuario.getPesoKg(),
                usuario.getAniosServicio());
    }
}
