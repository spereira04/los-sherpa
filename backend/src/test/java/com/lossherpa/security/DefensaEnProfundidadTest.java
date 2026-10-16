package com.lossherpa.security;

import com.lossherpa.support.CapturadorDeAuditoria;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RS01 - Defensa en profundidad: la segunda capa deniega aunque la regla de URL no este.
 *
 * Este contexto de test agrega, por delante de la cadena real, una cadena que deja pasar
 * sin ninguna regla todo /api/admin/**. Es el equivalente a comentar la regla de admin en
 * SecurityConfig. Aun asi, InterceptorPoliticaAcceso vuelve a consultar la politica sobre el
 * handler elegido por MVC y la request no llega al controlador.
 */
@Import(DefensaEnProfundidadTest.CadenaSinReglaDeAdmin.class)
class DefensaEnProfundidadTest extends TestIntegracion {

    @TestConfiguration
    static class CadenaSinReglaDeAdmin {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        SecurityFilterChain cadenaSinReglaDeAdmin(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/api/admin/**")
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    private MockHttpSession atleta;
    private MockHttpSession admin;

    @BeforeEach
    void datos() throws Exception {
        crearAtleta("ana@test.com", "Ana", "Suarez");
        crearAdmin("admin@test.com");
        atleta = iniciarSesion("ana@test.com");
        admin = iniciarSesion("admin@test.com");
    }

    @Test
    @DisplayName("RS01: sin la regla de URL, un atleta en /api/admin igual recibe 403 de la segunda capa")
    void sinReglaDeUrlElAtletaIgualEsDenegado() throws Exception {
        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            String respuesta = mvc.perform(get("/api/admin/usuarios").session(atleta))
                    .andExpect(status().isForbidden())
                    .andReturn().getResponse().getContentAsString();

            assertThat(respuesta).doesNotContain("ana@test.com");
            assertThat(auditoria.ultimaLinea())
                    .contains("\"estado\":403")
                    .contains("el endpoint exige el rol ADMIN");
        }
    }

    @Test
    @DisplayName("RS01: sin la regla de URL, una request sin sesion igual recibe 401 de la segunda capa")
    void sinReglaDeUrlSinSesionEs401() throws Exception {
        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
            assertThat(auditoria.ultimaLinea()).contains("\"estado\":401");
        }
    }

    @Test
    @DisplayName("RS01: la segunda capa no le saca nada al rol correcto")
    void elAdminSigueEntrando() throws Exception {
        mvc.perform(get("/api/admin/usuarios").session(admin)).andExpect(status().isOk());
    }
}
