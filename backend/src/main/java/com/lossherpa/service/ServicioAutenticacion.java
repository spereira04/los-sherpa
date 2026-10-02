package com.lossherpa.service;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;
import com.lossherpa.error.ConflictoException;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.web.dto.in.RegistroRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Registro publico. El login lo resuelve el AuthenticationManager en el controlador. */
@Service
public class ServicioAutenticacion {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasena politicaContrasena;

    public ServicioAutenticacion(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                                 PoliticaContrasena politicaContrasena) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.politicaContrasena = politicaContrasena;
    }

    @Transactional
    public Usuario registrar(RegistroRequest datos) {
        String email = ServicioUsuarios.normalizarEmail(datos.email());

        if (usuarios.existsByEmailIgnoreCase(email)) {
            // Mismo mensaje que usaria el formulario: el email es publico al intentar registrarse.
            throw new ConflictoException("Ya existe una cuenta con ese email");
        }

        // El tipo RolRegistro no incluye ADMIN: el registro no puede crear un admin.
        Rol rol = datos.rol().aRolDeDominio();

        CalculadoraEdad.validarFechaNacimiento(datos.fechaNacimiento());
        politicaContrasena.validar(datos.contrasena(),
                List.of(email, datos.nombre(), datos.apellido()));
        ServicioUsuarios.validarCamposPorRol(rol, datos.pesoKg(), datos.aniosServicio());

        Usuario usuario = new Usuario(
                email,
                passwordEncoder.encode(PoliticaContrasena.normalizar(datos.contrasena())),
                rol,
                datos.nombre().trim(),
                datos.apellido().trim(),
                datos.fechaNacimiento());
        usuario.setDescripcion(ServicioUsuarios.limpiarTextoOpcional(datos.descripcion()));

        if (rol == Rol.ATLETA) {
            usuario.setPesoKg(datos.pesoKg());
        } else {
            usuario.setAniosServicio(datos.aniosServicio());
        }

        return usuarios.save(usuario);
    }
}
