package com.lossherpa.service;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.web.dto.in.ActualizarPerfilRequest;
import com.lossherpa.web.dto.in.ActualizarPreferenciasRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Perfil propio.
 *
 * RS04: siempre opera sobre el usuario autenticado; no recibe ningun id del cliente.
 * Mass assignment: el email y el rol no se tocan nunca. Ademas de no estar en el DTO,
 * estan marcados updatable = false en la entidad.
 */
@Service
public class ServicioPerfil {

    private final UsuarioRepository usuarios;
    private final ServicioUsuarios servicioUsuarios;

    public ServicioPerfil(UsuarioRepository usuarios, ServicioUsuarios servicioUsuarios) {
        this.usuarios = usuarios;
        this.servicioUsuarios = servicioUsuarios;
    }

    @Transactional(readOnly = true)
    public Usuario miPerfil() {
        return servicioUsuarios.autenticado();
    }

    @Transactional
    public Usuario actualizar(ActualizarPerfilRequest datos) {
        Usuario usuario = servicioUsuarios.autenticado();

        CalculadoraEdad.validarFechaNacimiento(datos.fechaNacimiento());
        ServicioUsuarios.validarCamposPorRol(usuario.getRol(), datos.pesoKg(),
                datos.aniosServicio());

        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        usuario.setFechaNacimiento(datos.fechaNacimiento());
        usuario.setDescripcion(ServicioUsuarios.limpiarTextoOpcional(datos.descripcion()));
        // RF7: el telefono es opcional; vacio o null lo borra.
        usuario.setTelefono(ServicioUsuarios.limpiarTextoOpcional(datos.telefono()));

        if (usuario.getRol() == Rol.ATLETA) {
            usuario.setPesoKg(datos.pesoKg());
        } else if (usuario.getRol() == Rol.ENTRENADOR) {
            usuario.setAniosServicio(datos.aniosServicio());
        }

        return usuarios.save(usuario);
    }

    /** RF7 / RS04: solo el tema, siempre del usuario autenticado. */
    @Transactional
    public Usuario actualizarPreferencias(ActualizarPreferenciasRequest datos) {
        Usuario usuario = servicioUsuarios.autenticado();
        usuario.setTema(datos.tema());
        return usuarios.save(usuario);
    }
}
