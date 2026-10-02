package com.lossherpa.service;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;
import com.lossherpa.error.RecursoNoEncontradoException;
import com.lossherpa.error.ReglaNegocioException;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.security.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Utilidades compartidas sobre usuarios: resolucion del autenticado y reglas por rol. */
@Service
public class ServicioUsuarios {

    private final UsuarioRepository usuarios;

    public ServicioUsuarios(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /**
     * RS04: el usuario se resuelve desde el SecurityContext, nunca desde un id del cliente.
     * RS10: se lee de la base en cada llamada, sin cache.
     */
    @Transactional(readOnly = true)
    public Usuario autenticado() {
        return usuarios.findById(UsuarioActual.id())
                .orElseThrow(RecursoNoEncontradoException::generico);
    }

    /** Exige que el usuario autenticado tenga el rol esperado (chequeo redundante a proposito). */
    public Usuario autenticadoConRol(Rol esperado) {
        Usuario usuario = autenticado();
        if (usuario.getRol() != esperado) {
            // No deberia pasar: la cadena de filtros ya filtro por rol. Es defensa en profundidad.
            throw new org.springframework.security.access.AccessDeniedException(
                    "Rol incorrecto para esta operacion");
        }
        return usuario;
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String limpiarTextoOpcional(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    /**
     * Los campos especificos de cada rol no son intercambiables: un atleta que manda
     * aniosServicio o un entrenador que manda pesoKg recibe 400, no un silencio.
     */
    public static void validarCamposPorRol(Rol rol, Double pesoKg, Integer aniosServicio) {
        if (rol == Rol.ATLETA) {
            if (pesoKg == null) {
                throw new ReglaNegocioException("El peso en kg es obligatorio para un atleta");
            }
            if (aniosServicio != null) {
                throw new ReglaNegocioException(
                        "Los anos de servicio son un dato de entrenador, no de atleta");
            }
        } else if (rol == Rol.ENTRENADOR) {
            if (aniosServicio == null) {
                throw new ReglaNegocioException(
                        "Los anos de servicio son obligatorios para un entrenador");
            }
            if (pesoKg != null) {
                throw new ReglaNegocioException("El peso es un dato de atleta, no de entrenador");
            }
        } else {
            if (pesoKg != null || aniosServicio != null) {
                throw new ReglaNegocioException("Datos no aplicables a este rol");
            }
        }
    }
}
