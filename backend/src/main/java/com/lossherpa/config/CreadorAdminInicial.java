package com.lossherpa.config;

import com.lossherpa.domain.Rol;
import com.lossherpa.error.ReglaNegocioException;
import com.lossherpa.domain.Usuario;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.service.PoliticaContrasena;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * RS14 - Sin configuracion por defecto.
 *
 * El admin se crea al arrancar unicamente si estan definidas ADMIN_EMAIL y ADMIN_PASSWORD.
 * No hay credenciales por defecto en el codigo ni en los archivos de configuracion, y el
 * registro publico nunca puede crear un admin (ver ServicioAutenticacion).
 *
 * Si la contrasena provista no cumple la politica, la aplicacion no arranca: es preferible
 * a quedar con un admin debil.
 */
@Component
public class CreadorAdminInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CreadorAdminInicial.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasena politicaContrasena;
    private final String email;
    private final String password;
    private final String nombre;
    private final String apellido;

    public CreadorAdminInicial(UsuarioRepository usuarios,
                               PasswordEncoder passwordEncoder,
                               PoliticaContrasena politicaContrasena,
                               @Value("${ADMIN_EMAIL:}") String email,
                               @Value("${ADMIN_PASSWORD:}") String password,
                               @Value("${ADMIN_NOMBRE:Administrador}") String nombre,
                               @Value("${ADMIN_APELLIDO:del Sistema}") String apellido) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.politicaContrasena = politicaContrasena;
        this.email = email;
        this.password = password;
        this.nombre = nombre;
        this.apellido = apellido;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            LOG.info("ADMIN_EMAIL o ADMIN_PASSWORD sin definir: no se crea ningun admin");
            return;
        }
        if (usuarios.existsByEmailIgnoreCase(email)) {
            LOG.info("El admin inicial ya existe: no se crea de nuevo");
            return;
        }

        try {
            politicaContrasena.validar(password, List.of(email, nombre, apellido));
        } catch (ReglaNegocioException e) {
            // Preferible cortar el arranque antes que quedar con un admin debil. El mensaje
            // tiene que alcanzar para corregirlo sin leer el stack trace.
            throw new IllegalStateException(
                    "ADMIN_PASSWORD no cumple la politica de contrasenas: " + e.getMessage()
                    + ". Corregila y volve a arrancar (ver la seccion de politica de"
                    + " contrasenas del README). Ojo: no puede contener la parte local de"
                    + " ADMIN_EMAIL, ni ADMIN_NOMBRE, ni ADMIN_APELLIDO.", e);
        }

        Usuario admin = new Usuario(
                email.trim().toLowerCase(),
                passwordEncoder.encode(PoliticaContrasena.normalizar(password)),
                Rol.ADMIN,
                nombre,
                apellido,
                LocalDate.of(1990, 1, 1));
        usuarios.save(admin);
        // No se loguea el email completo del admin: alcanza con saber que se creo.
        LOG.info("Admin inicial creado a partir de las variables de entorno");
    }
}
