package com.lossherpa.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lossherpa.domain.Rol;
import com.lossherpa.domain.SolicitudVinculacion;
import com.lossherpa.domain.Usuario;
import com.lossherpa.domain.Vinculo;
import com.lossherpa.repository.SolicitudVinculacionRepository;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.repository.VinculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de los tests de integracion: contexto real, cadena de filtros de seguridad real y
 * MockMvc. La base se limpia antes de cada test para que no haya dependencia de orden.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class TestIntegracion {

    /** Cumple la politica de contrasenas y no contiene datos de ningun usuario de prueba. */
    public static final String PASSWORD = "sube la barra 90 kilos";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected UsuarioRepository usuarios;

    @Autowired
    protected VinculoRepository vinculos;

    @Autowired
    protected SolicitudVinculacionRepository solicitudes;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void limpiarBase() {
        // Orden: primero lo que referencia, despues lo referenciado.
        solicitudes.deleteAll();
        vinculos.deleteAll();
        usuarios.deleteAll();
    }

    protected Usuario crearAtleta(String email, String nombre, String apellido) {
        Usuario atleta = new Usuario(email, passwordEncoder.encode(PASSWORD), Rol.ATLETA,
                nombre, apellido, LocalDate.of(1998, 3, 12));
        atleta.setPesoKg(72.5);
        return usuarios.save(atleta);
    }

    protected Usuario crearEntrenador(String email, String nombre, String apellido) {
        Usuario entrenador = new Usuario(email, passwordEncoder.encode(PASSWORD), Rol.ENTRENADOR,
                nombre, apellido, LocalDate.of(1985, 7, 30));
        entrenador.setAniosServicio(9);
        entrenador.setDescripcion("Fuerza y acondicionamiento");
        return usuarios.save(entrenador);
    }

    protected Usuario crearAdmin(String email) {
        return usuarios.save(new Usuario(email, passwordEncoder.encode(PASSWORD), Rol.ADMIN,
                "Admin", "Sistema", LocalDate.of(1990, 1, 1)));
    }

    protected Vinculo crearVinculoActivo(Usuario entrenador, Usuario atleta) {
        return vinculos.save(new Vinculo(entrenador, atleta));
    }

    protected SolicitudVinculacion crearSolicitudPendiente(Usuario atleta, Usuario entrenador) {
        return solicitudes.save(new SolicitudVinculacion(atleta, entrenador));
    }

    /** Hace login de verdad por el endpoint y devuelve la sesion resultante. */
    protected MockHttpSession iniciarSesion(String email) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", email, "contrasena", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    protected String cuerpo(Object objeto) throws Exception {
        return json.writeValueAsString(objeto);
    }
}
