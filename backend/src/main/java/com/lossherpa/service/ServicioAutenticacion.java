package com.lossherpa.service;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;
import com.lossherpa.error.ConflictoException;
import com.lossherpa.error.CredencialesInvalidasException;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.security.UsuarioActual;
import com.lossherpa.web.dto.in.RegistroRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Registro publico y usuario de la sesion. El login lo resuelve el AuthenticationManager en
 * el controlador.
 */
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

    /**
     * RS16: el usuario de la sesion actual, para /api/auth/yo y la respuesta del login. El
     * id sale del SecurityContext (UsuarioActual), nunca del request. Si el usuario ya no
     * existe se responde como credenciales invalidas (401).
     */
    @Transactional(readOnly = true)
    public Usuario usuarioDeLaSesion() {
        return usuarios.findById(UsuarioActual.id())
                .orElseThrow(CredencialesInvalidasException::new);
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

        char[] normalizada = PoliticaContrasena.normalizar(datos.contrasena());
        String hash;
        try {
            hash = passwordEncoder.encode(CharBuffer.wrap(normalizada));
        } finally {
            Arrays.fill(normalizada, '\0');
            Arrays.fill(datos.contrasena(), '\0');
        }

        Usuario usuario = new Usuario(
                email,
                hash,
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
