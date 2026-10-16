package com.lossherpa.web;

import com.lossherpa.domain.Usuario;
import com.lossherpa.support.CapturadorDeAuditoria;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RS02 - Deny by default para requests no contempladas.
 *
 * Una request que no coincide exactamente con un endpoint declarado (metodo + path) la
 * deniega la capa de autorizacion, no el ruteo de MVC, y queda auditada como
 * "request no contemplada". Autenticado: 403. Sin sesion, dentro de una zona que exige
 * sesion: 401.
 */
class DenegacionPorDefectoTest extends TestIntegracion {

    private MockHttpSession atleta;
    private MockHttpSession entrenador;
    private Usuario atletaDelEntrenador;

    @BeforeEach
    void datos() throws Exception {
        atletaDelEntrenador = crearAtleta("ana@test.com", "Ana", "Suarez");
        Usuario coach = crearEntrenador("coach@test.com", "Pablo", "Paz");
        crearVinculoActivo(coach, atletaDelEntrenador);
        atleta = iniciarSesion("ana@test.com");
        entrenador = iniciarSesion("coach@test.com");
    }

    @Test
    @DisplayName("RS02: path inexistente bajo un prefijo valido -> 403 desde la capa de seguridad")
    void pathInexistenteBajoPrefijoValido() throws Exception {
        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(get("/api/atleta/no-existe").session(atleta))
                    .andExpect(status().isForbidden());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"estado\":403")
                    .contains("\"recurso\":\"/api/atleta/no-existe\"")
                    .contains("request no contemplada");
        }
    }

    @Test
    @DisplayName("RS02: metodo no declarado sobre /api/perfil -> 403 y auditoria")
    void metodoNoDeclaradoSobrePerfil() throws Exception {
        for (HttpMethod metodo : new HttpMethod[] {HttpMethod.PATCH, HttpMethod.DELETE}) {
            try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
                mvc.perform(conCuerpo(request(metodo, "/api/perfil")).session(atleta))
                        .andExpect(status().isForbidden());

                assertThat(auditoria.ultimaLinea())
                        .as("auditoria de %s /api/perfil", metodo)
                        .contains("\"metodo\":\"" + metodo.name() + "\"")
                        .contains("request no contemplada");
            }
        }
    }

    @Test
    @DisplayName("RS02: PUT no declarado sobre las rutinas de un atleta -> 403")
    void putNoDeclaradoDelEntrenador() throws Exception {
        String ruta = "/api/entrenador/atletas/" + atletaDelEntrenador.getId() + "/rutinas";
        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(conCuerpo(request(HttpMethod.PUT, ruta)).session(entrenador))
                    .andExpect(status().isForbidden());

            assertThat(auditoria.ultimaLinea()).contains("request no contemplada");
        }
    }

    @Test
    @DisplayName("RS02: sin sesion, las mismas requests responden 401")
    void sinSesionEs401() throws Exception {
        String ruta = "/api/entrenador/atletas/" + UUID.randomUUID() + "/rutinas";
        mvc.perform(get("/api/atleta/no-existe")).andExpect(status().isUnauthorized());
        mvc.perform(conCuerpo(request(HttpMethod.PATCH, "/api/perfil")))
                .andExpect(status().isUnauthorized());
        mvc.perform(conCuerpo(request(HttpMethod.DELETE, "/api/perfil")))
                .andExpect(status().isUnauthorized());
        mvc.perform(conCuerpo(request(HttpMethod.PUT, ruta)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RS02: TRACE lo rechaza el firewall de Spring Security, antes de autorizar, y se audita")
    void traceLoRechazaElFirewall() throws Exception {
        for (MockHttpSession sesion : new MockHttpSession[] {atleta, null}) {
            try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
                MockHttpServletRequestBuilder trace = request(HttpMethod.TRACE, "/api/perfil");
                if (sesion != null) {
                    trace.session(sesion);
                }
                String respuesta = mvc.perform(trace)
                        .andExpect(status().isBadRequest())
                        .andReturn().getResponse().getContentAsString();

                assertThat(respuesta).doesNotContain("Exception");
                assertThat(auditoria.ultimaLinea())
                        .contains("\"metodo\":\"TRACE\"")
                        .contains("request rechazada por el firewall");
            }
        }
    }

    @Test
    @DisplayName("RS02: un path con '..' o ';' lo rechaza el firewall y se audita")
    void pathsManipuladosLosRechazaElFirewall() throws Exception {
        for (String ruta : new String[] {
                "/api/atleta/../admin/usuarios", "/api/perfil;jsessionid=x"
        }) {
            try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
                mvc.perform(get(ruta).session(atleta)).andExpect(status().isBadRequest());
                assertThat(auditoria.ultimaLinea()).contains("request rechazada por el firewall");
            }
        }
    }

    @Test
    @DisplayName("RS02: HEAD no esta declarado, aunque MVC lo atenderia con el handler GET")
    void headNoDeclaradoSeDeniega() throws Exception {
        mvc.perform(request(HttpMethod.HEAD, "/api/atleta/rutinas").session(atleta))
                .andExpect(status().isForbidden());
        mvc.perform(request(HttpMethod.HEAD, "/api/perfil").session(atleta))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RS02: OPTIONS fuera de un preflight CORS se deniega")
    void optionsSinCorsSeDeniega() throws Exception {
        mvc.perform(options("/api/atleta/rutinas").session(atleta))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RS02: el preflight CORS del origen permitido sigue funcionando")
    void elPreflightCorsSigueFuncionando() throws Exception {
        mvc.perform(options("/api/perfil")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RS02: una ruta del SPA con un metodo que no es GET se deniega (no da 500)")
    void rutaDelSpaConOtroMetodoSeDeniega() throws Exception {
        for (HttpMethod metodo : new HttpMethod[] {HttpMethod.POST, HttpMethod.PUT,
                HttpMethod.DELETE}) {
            mvc.perform(request(metodo, "/login").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("RS02: los endpoints legitimos siguen respondiendo igual")
    void losEndpointsLegitimosSiguenIgual() throws Exception {
        mvc.perform(get("/api/perfil").session(atleta)).andExpect(status().isOk());
        mvc.perform(get("/api/atleta/rutinas").session(atleta)).andExpect(status().isOk());
        mvc.perform(get("/api/entrenador/atletas").session(entrenador)).andExpect(status().isOk());
        mvc.perform(get("/api/csrf")).andExpect(status().isOk());
    }

    /** Las requests que modifican llevan CSRF y un body, para que lo unico que falte sea la regla. */
    private MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder request) {
        return request.with(csrf()).contentType(APPLICATION_JSON).content("{}");
    }
}
