package com.lossherpa.web;

import com.lossherpa.domain.Rol;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RS15: registro publico, con foco en mass assignment y en la politica de contrasenas. */
class RegistroTest extends TestIntegracion {

    private Map<String, Object> registroAtleta() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("email", "nuevo.atleta@test.com");
        datos.put("contrasena", PASSWORD);
        datos.put("rol", "ATLETA");
        datos.put("nombre", "Lucia");
        datos.put("apellido", "Ferreyra");
        datos.put("fechaNacimiento", "2000-05-14");
        datos.put("pesoKg", 64.0);
        return datos;
    }

    private Map<String, Object> registroEntrenador() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("email", "nuevo.entrenador@test.com");
        datos.put("contrasena", PASSWORD);
        datos.put("rol", "ENTRENADOR");
        datos.put("nombre", "Marcos");
        datos.put("apellido", "Quiroga");
        datos.put("fechaNacimiento", "1980-11-02");
        datos.put("aniosServicio", 12);
        return datos;
    }

    @Test
    @DisplayName("registra un atleta y guarda el rol y el peso")
    void registraAtleta() throws Exception {
        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(registroAtleta())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ATLETA"))
                .andExpect(jsonPath("$.id").exists());

        var guardado = usuarios.findByEmailIgnoreCase("nuevo.atleta@test.com").orElseThrow();
        assertThat(guardado.getRol()).isEqualTo(Rol.ATLETA);
        assertThat(guardado.getPesoKg()).isEqualTo(64.0);
        assertThat(guardado.getAniosServicio()).isNull();
    }

    @Test
    @DisplayName("registra un entrenador y guarda los anos de servicio")
    void registraEntrenador() throws Exception {
        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(registroEntrenador())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ENTRENADOR"));

        var guardado = usuarios.findByEmailIgnoreCase("nuevo.entrenador@test.com").orElseThrow();
        assertThat(guardado.getAniosServicio()).isEqualTo(12);
        assertThat(guardado.getPesoKg()).isNull();
    }

    @Test
    @DisplayName("la respuesta del registro nunca incluye el hash de la contrasena")
    void laRespuestaNoFiltraElHash() throws Exception {
        String respuesta = mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(registroAtleta())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta).doesNotContain("password", "passwordHash", "$2a$", "$2b$");
    }

    @Test
    @DisplayName("no se puede registrar un admin: el rol ADMIN no es deserializable")
    void noSePuedeRegistrarUnAdmin() throws Exception {
        Map<String, Object> datos = registroAtleta();
        datos.put("rol", "ADMIN");

        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        assertThat(usuarios.findAll()).noneMatch(u -> u.getRol() == Rol.ADMIN);
    }

    @Test
    @DisplayName("mandar un rol inexistente tampoco crea nada")
    void rolInexistenteNoCreaNada() throws Exception {
        Map<String, Object> datos = registroAtleta();
        datos.put("rol", "SUPERADMIN");

        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        assertThat(usuarios.count()).isZero();
    }

    @Test
    @DisplayName("un campo fuera de la lista blanca (id) hace fallar el registro")
    void campoNoPermitidoEsRechazado() throws Exception {
        Map<String, Object> datos = registroAtleta();
        datos.put("id", "11111111-1111-4111-8111-111111111111");

        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        assertThat(usuarios.count()).isZero();
    }

    @Test
    @DisplayName("mandar passwordHash directo no tiene efecto: el campo no existe en el DTO")
    void passwordHashNoSePuedeInyectar() throws Exception {
        Map<String, Object> datos = registroAtleta();
        datos.put("passwordHash", "hash-elegido-por-el-atacante");

        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        assertThat(usuarios.count()).isZero();
    }

    @Test
    @DisplayName("rechaza un email ya registrado con 409")
    void rechazaEmailDuplicado() throws Exception {
        crearAtleta("nuevo.atleta@test.com", "Lucia", "Ferreyra");

        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(registroAtleta())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe una cuenta con ese email"));
    }

    @Test
    @DisplayName("el email se normaliza a minusculas y no se puede duplicar cambiando el case")
    void emailNoDistingueMayusculas() throws Exception {
        crearAtleta("lucia@test.com", "Lucia", "Ferreyra");
        Map<String, Object> datos = registroAtleta();
        datos.put("email", "LUCIA@TEST.COM");

        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("rechaza una contrasena que no cumple la politica, con mensaje util")
    void rechazaContrasenaDebil() throws Exception {
        Map<String, Object> datos = registroAtleta();
        datos.put("contrasena", "corta123");

        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("al menos 12")));

        assertThat(usuarios.count()).isZero();
    }

    @Test
    @DisplayName("un atleta no puede mandar anos de servicio")
    void atletaNoPuedeMandarAniosServicio() throws Exception {
        Map<String, Object> datos = registroAtleta();
        datos.put("aniosServicio", 10);

        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("un entrenador no puede mandar peso")
    void entrenadorNoPuedeMandarPeso() throws Exception {
        Map<String, Object> datos = registroEntrenador();
        datos.put("pesoKg", 80.0);

        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rechaza a un menor de 13 anos")
    void rechazaMenorDeTrece() throws Exception {
        Map<String, Object> datos = registroAtleta();
        datos.put("fechaNacimiento", "2020-01-01");

        mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("informa los campos invalidos sin devolver el valor recibido")
    void informaCamposInvalidos() throws Exception {
        Map<String, Object> datos = registroAtleta();
        datos.put("email", "no-es-un-email");

        String respuesta = mvc.perform(post("/api/auth/registro").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta).doesNotContain("no-es-un-email");
    }
}
