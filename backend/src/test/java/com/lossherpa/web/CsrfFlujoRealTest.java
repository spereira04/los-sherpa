package com.lossherpa.web;

import com.lossherpa.support.TestIntegracion;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo CSRF tal como lo hace el SPA: GET /api/csrf deja la cookie XSRF-TOKEN y el cliente
 * la reenvia en la cabecera X-XSRF-TOKEN.
 *
 * Esta clase NO usa el post-processor csrf() de spring-security-test a proposito: ese helper
 * reemplaza de forma permanente el CsrfTokenRepository del CsrfFilter en el contexto
 * compartido, y despues ninguna request vuelve a recibir la cookie real. Por eso ademas pide
 * un contexto fresco con @DirtiesContext.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfFlujoRealTest extends TestIntegracion {

    @BeforeEach
    void datos() {
        crearAtleta("ana@test.com", "Ana", "Suarez");
    }

    @Test
    @DisplayName("GET /api/csrf deja la cookie XSRF-TOKEN legible por el SPA")
    void entregaLaCookieLegible() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.nombreCabecera").value("X-XSRF-TOKEN"))
                .andReturn();

        Cookie cookie = resultado.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly())
                .as("el SPA tiene que poder leerla para reenviarla en la cabecera")
                .isFalse();
    }

    @Test
    @DisplayName("con la cookie y la cabecera correctas el login funciona")
    void conCookieYCabeceraFunciona() throws Exception {
        MvcResult resultadoCsrf = mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = resultadoCsrf.getResponse().getCookie("XSRF-TOKEN");
        String token = json.readTree(resultadoCsrf.getResponse().getContentAsString())
                .get("token").asText();

        mvc.perform(post("/api/auth/login")
                        .cookie(cookie)
                        .header("X-XSRF-TOKEN", token)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", "ana@test.com", "contrasena", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("una cabecera que no coincide con la cookie se rechaza con 403")
    void cabeceraQueNoCoincideSeRechaza() throws Exception {
        MvcResult resultadoCsrf = mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = resultadoCsrf.getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/auth/login")
                        .cookie(cookie)
                        .header("X-XSRF-TOKEN", "token-inventado")
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", "ana@test.com", "contrasena", PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("mandar solo la cookie, sin la cabecera, tampoco alcanza")
    void soloLaCookieNoAlcanza() throws Exception {
        MvcResult resultadoCsrf = mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = resultadoCsrf.getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/auth/login")
                        .cookie(cookie)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("email", "ana@test.com", "contrasena", PASSWORD))))
                .andExpect(status().isForbidden());
    }
}
