package com.lossherpa.web;

import com.lossherpa.domain.Rol;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RS15: perfil propio, con foco en mass assignment y en la edad calculada. */
class PerfilTest extends TestIntegracion {

    @BeforeEach
    void datos() {
        crearAtleta("ana@test.com", "Ana", "Suarez");
        crearEntrenador("beto@test.com", "Beto", "Lopez");
    }

    private Map<String, Object> perfilAtleta() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Ana Maria");
        datos.put("apellido", "Suarez");
        datos.put("fechaNacimiento", "1998-03-12");
        datos.put("descripcion", "Corro medias maratones");
        datos.put("pesoKg", 70.0);
        return datos;
    }

    @Test
    @DisplayName("sin sesion no se puede ver el perfil")
    void sinSesionNoHayPerfil() throws Exception {
        mvc.perform(get("/api/perfil")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("devuelve el perfil propio con la edad calculada y sin el hash")
    void devuelveElPerfilPropio() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        String respuesta = mvc.perform(get("/api/perfil").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.rol").value("ATLETA"))
                .andExpect(jsonPath("$.edad").isNumber())
                .andExpect(jsonPath("$.pesoKg").value(72.5))
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta).doesNotContain("passwordHash", "$2a$", "$2b$");
    }

    @Test
    @DisplayName("la edad no se persiste: se calcula desde la fecha de nacimiento")
    void laEdadSeCalcula() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        // Fecha relativa a hoy para que el test no caduque al cambiar de ano.
        Map<String, Object> datos = perfilAtleta();
        datos.put("fechaNacimiento", LocalDate.now().minusYears(25).toString());

        mvc.perform(put("/api/perfil").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edad").value(25));
    }

    @Test
    @DisplayName("actualiza los campos editables")
    void actualizaLosCamposEditables() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(put("/api/perfil").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(perfilAtleta())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Maria"));

        var guardada = usuarios.findByEmailIgnoreCase("ana@test.com").orElseThrow();
        assertThat(guardada.getNombre()).isEqualTo("Ana Maria");
        assertThat(guardada.getPesoKg()).isEqualTo(70.0);
    }

    @Test
    @DisplayName("mandar rol en el perfil no tiene efecto: el campo no esta en la lista blanca")
    void mandarRolNoTieneEfecto() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        Map<String, Object> datos = perfilAtleta();
        datos.put("rol", "ADMIN");

        mvc.perform(put("/api/perfil").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        var guardada = usuarios.findByEmailIgnoreCase("ana@test.com").orElseThrow();
        assertThat(guardada.getRol()).isEqualTo(Rol.ATLETA);
    }

    @Test
    @DisplayName("mandar email en el perfil no tiene efecto: el email es inmutable")
    void mandarEmailNoTieneEfecto() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        Map<String, Object> datos = perfilAtleta();
        datos.put("email", "otro@test.com");

        mvc.perform(put("/api/perfil").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        assertThat(usuarios.findByEmailIgnoreCase("ana@test.com")).isPresent();
        assertThat(usuarios.findByEmailIgnoreCase("otro@test.com")).isEmpty();
    }

    @Test
    @DisplayName("mandar id en el perfil no tiene efecto")
    void mandarIdNoTieneEfecto() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        var otro = usuarios.findByEmailIgnoreCase("beto@test.com").orElseThrow();
        Map<String, Object> datos = perfilAtleta();
        datos.put("id", otro.getId().toString());

        mvc.perform(put("/api/perfil").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        // El perfil del otro usuario quedo intacto.
        assertThat(usuarios.findById(otro.getId()).orElseThrow().getNombre()).isEqualTo("Beto");
    }

    @Test
    @DisplayName("mandar passwordHash en el perfil no tiene efecto")
    void mandarPasswordHashNoTieneEfecto() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        String hashOriginal = usuarios.findByEmailIgnoreCase("ana@test.com")
                .orElseThrow().getPasswordHash();
        Map<String, Object> datos = perfilAtleta();
        datos.put("passwordHash", "hash-del-atacante");

        mvc.perform(put("/api/perfil").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        assertThat(usuarios.findByEmailIgnoreCase("ana@test.com").orElseThrow().getPasswordHash())
                .isEqualTo(hashOriginal);
    }

    @Test
    @DisplayName("un atleta no puede agregarse anos de servicio")
    void atletaNoPuedeSetearAniosServicio() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        Map<String, Object> datos = perfilAtleta();
        datos.put("aniosServicio", 15);

        mvc.perform(put("/api/perfil").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        assertThat(usuarios.findByEmailIgnoreCase("ana@test.com").orElseThrow()
                .getAniosServicio()).isNull();
    }

    @Test
    @DisplayName("un entrenador no puede agregarse peso")
    void entrenadorNoPuedeSetearPeso() throws Exception {
        MockHttpSession sesion = iniciarSesion("beto@test.com");
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Beto");
        datos.put("apellido", "Lopez");
        datos.put("fechaNacimiento", "1985-07-30");
        datos.put("aniosServicio", 10);
        datos.put("pesoKg", 88.0);

        mvc.perform(put("/api/perfil").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        assertThat(usuarios.findByEmailIgnoreCase("beto@test.com").orElseThrow()
                .getPesoKg()).isNull();
    }

    @Test
    @DisplayName("rechaza una fecha de nacimiento futura")
    void rechazaFechaFutura() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        Map<String, Object> datos = perfilAtleta();
        datos.put("fechaNacimiento", "2030-01-01");

        mvc.perform(put("/api/perfil").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("una mutacion del perfil sin token CSRF se rechaza")
    void sinCsrfNoSePuedeActualizar() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(put("/api/perfil").session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(perfilAtleta())))
                .andExpect(status().isForbidden());
    }
}
