package com.lossherpa.web;

import com.lossherpa.support.CapturadorDeAuditoria;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RS15: login, sesion del lado del servidor, CSRF y logout. */
class LoginSesionTest extends TestIntegracion {

    @BeforeEach
    void datos() {
        crearAtleta("ana@test.com", "Ana", "Suarez");
    }

    @Test
    @DisplayName("login correcto devuelve el rol para que el frontend elija el panel")
    void loginCorrecto() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", "ana@test.com", "contrasena", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ATLETA"))
                .andExpect(jsonPath("$.email").value("ana@test.com"));
    }

    @Test
    @DisplayName("el login acepta el email con otro case")
    void loginNoDistingueMayusculas() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", "ANA@TEST.COM", "contrasena", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("la sesion sirve para el request siguiente")
    void laSesionPersiste() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/auth/yo").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@test.com"));
    }

    @Test
    @DisplayName("contrasena incorrecta devuelve 401 con mensaje generico")
    void contrasenaIncorrecta() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", "ana@test.com",
                                "contrasena", "otra contrasena larga"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Email o contrasena incorrectos"));
    }

    @Test
    @DisplayName("email inexistente devuelve el mismo mensaje: no revela si la cuenta existe")
    void emailInexistenteMismoMensaje() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", "nadie@test.com", "contrasena", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Email o contrasena incorrectos"));
    }

    @Test
    @DisplayName("RS12: el login fallido queda logueado con el email pero nunca la contrasena")
    void elLoginFallidoSeLogueaSinLaContrasena() throws Exception {
        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(cuerpo(Map.of("email", "ana@test.com",
                                    "contrasena", "clave secreta del atacante"))))
                    .andExpect(status().isUnauthorized());

            String linea = auditoria.ultimaLinea();
            assertThat(linea).contains("\"evento\":\"login_fallido\"")
                    .contains("ana@test.com")
                    .contains("\"ip\":")
                    .doesNotContain("clave secreta del atacante");
        }
    }

    @Test
    @DisplayName("RS12/RS17: un email con saltos de linea no puede inyectar una entrada de log")
    void noSePuedeInyectarEnElLog() throws Exception {
        String emailMalicioso = "falso@test.com\n{\"evento\":\"login_ok\",\"usuarioId\":\"admin\"}";

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(cuerpo(Map.of("email", emailMalicioso,
                                    "contrasena", PASSWORD))))
                    .andExpect(status().isUnauthorized());

            // Lo que importa no es que el texto del atacante desaparezca, sino que no pueda
            // convertirse en un registro aparte: cada linea sigue siendo UN solo JSON y su
            // evento es el real. El payload queda encerrado dentro del campo "email".
            assertThat(auditoria.lineas()).isNotEmpty().allSatisfy(linea -> {
                assertThat(linea).doesNotContain("\n").doesNotContain("\r");
                assertThat(json.readTree(linea).get("evento").asText())
                        .isEqualTo("login_fallido");
            });
        }
    }

    @Test
    @DisplayName("un request sin sesion a un endpoint protegido devuelve 401")
    void sinSesionDevuelve401() throws Exception {
        mvc.perform(get("/api/perfil"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("No autenticado"));
    }

    @Test
    @DisplayName("RS14: el 401 no ofrece basic auth ni expone detalles internos")
    void el401NoOfreceBasicAuth() throws Exception {
        mvc.perform(get("/api/perfil"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    @DisplayName("RS12: el acceso sin sesion deja una entrada de auditoria sin datos sensibles")
    void elAccesoSinSesionSeAudita() throws Exception {
        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(get("/api/perfil")).andExpect(status().isUnauthorized());

            String linea = auditoria.ultimaLinea();
            assertThat(linea).contains("\"evento\":\"acceso_denegado\"")
                    .contains("\"estado\":401")
                    .contains("\"usuarioId\":\"anonimo\"")
                    .contains("\"recurso\":\"/api/perfil\"")
                    .doesNotContain(PASSWORD)
                    .doesNotContain("SHERPASESSION")
                    .doesNotContain("XSRF");
        }
    }

    @Test
    @DisplayName("RS12: el 403 por rol registra el id del usuario autenticado, no 'anonimo'")
    void elAccesoFueraDeRolRegistraElUsuario() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        var ana = usuarios.findByEmailIgnoreCase("ana@test.com").orElseThrow();

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(get("/api/admin/usuarios").session(sesion))
                    .andExpect(status().isForbidden());

            String linea = auditoria.ultimaLinea();
            assertThat(linea).contains("\"evento\":\"acceso_denegado\"")
                    .contains("\"estado\":403")
                    .contains(ana.getId().toString())
                    .doesNotContain("anonimo")
                    .doesNotContain("ana@test.com")
                    .doesNotContain(PASSWORD);
        }
    }

    @Test
    @DisplayName("el login regenera el id de sesion (fijacion de sesion)")
    void elLoginRegeneraElIdDeSesion() throws Exception {
        MockHttpSession sesionPrevia = new MockHttpSession();
        String idPrevio = sesionPrevia.getId();

        mvc.perform(post("/api/auth/login").with(csrf())
                        .session(sesionPrevia)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", "ana@test.com", "contrasena", PASSWORD))))
                .andExpect(status().isOk());

        assertThat(sesionPrevia.getId()).isNotEqualTo(idPrevio);
    }

    @Test
    @DisplayName("el logout invalida la sesion: el request siguiente da 401")
    void elLogoutInvalidaLaSesion() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(post("/api/auth/logout").with(csrf()).session(sesion))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/yo").session(sesion))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("una mutacion sin token CSRF se rechaza con 403")
    void sinTokenCsrfDevuelve403() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", "ana@test.com", "contrasena", PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RS14: ninguna respuesta de error trae stack trace")
    void lasRespuestasDeErrorNoTraenStackTrace() throws Exception {
        String sinSesion = mvc.perform(get("/api/perfil"))
                .andReturn().getResponse().getContentAsString();
        String credencialesMal = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", "ana@test.com", "contrasena", "mal mal mal"))))
                .andReturn().getResponse().getContentAsString();
        String cuerpoIlegible = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{esto no es json"))
                .andReturn().getResponse().getContentAsString();

        assertThat(sinSesion + credencialesMal + cuerpoIlegible)
                .doesNotContain("Exception")
                .doesNotContain("at com.lossherpa")
                .doesNotContain("org.springframework")
                .doesNotContain("java.lang")
                .doesNotContain("trace");
    }
}
