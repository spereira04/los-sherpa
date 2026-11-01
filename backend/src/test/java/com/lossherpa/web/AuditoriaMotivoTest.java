package com.lossherpa.web;

import com.lossherpa.domain.Rutina;
import com.lossherpa.domain.Usuario;
import com.lossherpa.support.CapturadorDeAuditoria;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RS04 + RS12: el 404 es el mismo para el cliente, pero la auditoria registra el motivo real
 * (recurso inexistente, ajeno, archivado o sin vinculo vigente).
 */
class AuditoriaMotivoTest extends TestIntegracion {

    private Usuario ana;
    private Usuario carla;
    private Usuario beto;

    @BeforeEach
    void datos() {
        ana = crearAtleta("ana@test.com", "Ana", "Suarez");
        carla = crearAtleta("carla@test.com", "Carla", "Rios");
        beto = crearEntrenador("beto@test.com", "Beto", "Lopez");
    }

    @Test
    @DisplayName("rutina inexistente y rutina ajena: misma respuesta, distinto motivo")
    void rutinaInexistenteVsAjena() throws Exception {
        Rutina deAna = crearRutinaPropia(ana, "Fuerza");
        MockHttpSession sesion = iniciarSesion("carla@test.com");

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            String inexistente = mvc.perform(get("/api/atleta/rutinas/" + UUID.randomUUID())
                            .session(sesion))
                    .andExpect(status().isNotFound())
                    .andReturn().getResponse().getContentAsString();
            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"acceso_denegado\"")
                    .contains("\"motivo\":\"rutina inexistente\"");

            String ajena = mvc.perform(get("/api/atleta/rutinas/" + deAna.getId())
                            .session(sesion))
                    .andExpect(status().isNotFound())
                    .andReturn().getResponse().getContentAsString();
            assertThat(auditoria.ultimaLinea())
                    .contains("\"usuarioId\":\"" + carla.getId() + "\"")
                    .contains("\"motivo\":\"rutina de otro usuario\"");

            // RS07: el cliente no puede distinguir los dos casos.
            assertThat(ajena).isEqualTo(inexistente);
        }
    }

    @Test
    @DisplayName("una rutina propia archivada se audita como archivada, no como ajena")
    void rutinaPropiaArchivada() throws Exception {
        Rutina rutina = crearRutinaAsignada(beto, ana, "Base");
        rutina.archivar();
        rutinas.save(rutina);

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(get("/api/atleta/rutinas/" + rutina.getId())
                            .session(iniciarSesion("ana@test.com")))
                    .andExpect(status().isNotFound());

            assertThat(auditoria.ultimaLinea()).contains("\"motivo\":\"rutina archivada\"");
        }
    }

    @Test
    @DisplayName("borrar la ejecucion de otro atleta se audita como ajena")
    void ejecucionAjena() throws Exception {
        var ejecucion = crearEjecucion(crearRutinaPropia(ana, "Fuerza"), LocalDate.now(), 40);

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(delete("/api/atleta/ejecuciones/" + ejecucion.getId())
                            .with(csrf()).session(iniciarSesion("carla@test.com")))
                    .andExpect(status().isNotFound());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"motivo\":\"ejecucion de otro usuario\"");
        }
        assertThat(ejecuciones.existsById(ejecucion.getId())).isTrue();
    }

    @Test
    @DisplayName("el entrenador sin vinculo con el atleta queda auditado con ese motivo")
    void entrenadorSinVinculo() throws Exception {
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(get("/api/entrenador/atletas/" + ana.getId()).session(sesion))
                    .andExpect(status().isNotFound());
            assertThat(auditoria.ultimaLinea())
                    .contains("\"motivo\":\"sin vinculo vigente con ese atleta\"");

            mvc.perform(get("/api/entrenador/atletas/" + UUID.randomUUID()).session(sesion))
                    .andExpect(status().isNotFound());
            assertThat(auditoria.ultimaLinea()).contains("\"motivo\":\"atleta inexistente\"");
        }
    }
}
