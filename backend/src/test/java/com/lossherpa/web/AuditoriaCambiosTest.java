package com.lossherpa.web;

import com.lossherpa.domain.Usuario;
import com.lossherpa.support.CapturadorDeAuditoria;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RS12 + RS10: cada cambio de autorizacion y cada accion del admin deja una linea de auditoria. */
class AuditoriaCambiosTest extends TestIntegracion {

    private Usuario admin;
    private Usuario atleta;
    private Usuario entrenador;

    @BeforeEach
    void datos() {
        admin = crearAdmin("admin@test.com");
        atleta = crearAtleta("ana@test.com", "Ana", "Suarez");
        entrenador = crearEntrenador("beto@test.com", "Beto", "Lopez");
    }

    @Test
    @DisplayName("el admin que elimina un usuario queda auditado con actor y afectado")
    void bajaPorAdmin() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(delete("/api/admin/usuarios/" + atleta.getId())
                            .with(csrf()).session(sesion))
                    .andExpect(status().isNoContent());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"usuario_eliminado\"")
                    .contains("\"actorId\":\"" + admin.getId() + "\"")
                    .contains("\"afectadoId\":\"" + atleta.getId() + "\"")
                    .contains("\"ts\"")
                    .doesNotContain("ana@test.com");
        }
    }

    @Test
    @DisplayName("aceptar una solicitud audita vinculo_creado")
    void aceptarAudita() throws Exception {
        var solicitud = crearSolicitudPendiente(atleta, entrenador);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(post("/api/entrenador/solicitudes/" + solicitud.getId() + "/aceptar")
                            .with(csrf()).session(sesion))
                    .andExpect(status().is2xxSuccessful());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"vinculo_creado\"")
                    .contains("\"actorId\":\"" + entrenador.getId() + "\"")
                    .contains("\"afectadoId\":\"" + atleta.getId() + "\"");
        }
    }

    @Test
    @DisplayName("rechazar una solicitud audita solicitud_rechazada")
    void rechazarAudita() throws Exception {
        var solicitud = crearSolicitudPendiente(atleta, entrenador);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(post("/api/entrenador/solicitudes/" + solicitud.getId() + "/rechazar")
                            .with(csrf()).session(sesion))
                    .andExpect(status().is2xxSuccessful());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"solicitud_rechazada\"")
                    .contains("\"actorId\":\"" + entrenador.getId() + "\"")
                    .contains("\"afectadoId\":\"" + atleta.getId() + "\"");
        }
    }

    @Test
    @DisplayName("la desvinculacion por el atleta audita vinculo_cerrado")
    void desvincularAudita() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(delete("/api/atleta/vinculo").with(csrf()).session(sesion))
                    .andExpect(status().isNoContent());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"vinculo_cerrado\"")
                    .contains("\"actorId\":\"" + atleta.getId() + "\"")
                    .contains("\"afectadoId\":\"" + entrenador.getId() + "\"");
        }
    }

    @Test
    @DisplayName("login exitoso y logout quedan auditados sin credenciales")
    void loginYLogout() throws Exception {
        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            MockHttpSession sesion = iniciarSesion("ana@test.com");

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"login_exitoso\"")
                    .contains(atleta.getId().toString())
                    .doesNotContain(PASSWORD)
                    .doesNotContain("ana@test.com");

            mvc.perform(post("/api/auth/logout").with(csrf()).session(sesion))
                    .andExpect(status().isNoContent());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"logout\"")
                    .contains(atleta.getId().toString());
        }
    }
}
