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

    // --- RF7: tema ---

    private void guardarTema(MockHttpSession sesion, String tema, int estado) throws Exception {
        mvc.perform(put("/api/perfil/preferencias").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("tema", tema))))
                .andExpect(status().is(estado));
    }

    @Test
    @DisplayName("RF7: el tema por defecto es SISTEMA")
    void temaPorDefecto() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        mvc.perform(get("/api/perfil").session(sesion))
                .andExpect(jsonPath("$.tema").value("SISTEMA"));
    }

    @Test
    @DisplayName("RF7: el tema guardado se devuelve en el perfil y en la sesion")
    void guardaElTema() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        guardarTema(sesion, "OSCURO", 200);

        mvc.perform(get("/api/perfil").session(sesion))
                .andExpect(jsonPath("$.tema").value("OSCURO"));
        mvc.perform(get("/api/auth/yo").session(sesion))
                .andExpect(jsonPath("$.tema").value("OSCURO"));
        // Persiste entre sesiones.
        mvc.perform(get("/api/auth/yo").session(iniciarSesion("ana@test.com")))
                .andExpect(jsonPath("$.tema").value("OSCURO"));
    }

    @Test
    @DisplayName("RF7: el admin tambien puede elegir tema")
    void adminElijeTema() throws Exception {
        crearAdmin("admin@test.com");
        guardarTema(iniciarSesion("admin@test.com"), "CLARO", 200);
    }

    @Test
    @DisplayName("RF7: un tema invalido se rechaza")
    void temaInvalido() throws Exception {
        guardarTema(iniciarSesion("ana@test.com"), "ROJO", 400);
    }

    @Test
    @DisplayName("RF7: las preferencias no aceptan campos extra (rol, email, telefono)")
    void preferenciasConCamposExtra() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        for (String campo : new String[] {"rol", "email", "telefono"}) {
            Map<String, Object> datos = new HashMap<>();
            datos.put("tema", "OSCURO");
            datos.put(campo, "ADMIN");
            mvc.perform(put("/api/perfil/preferencias").with(csrf()).session(sesion)
                            .contentType(APPLICATION_JSON)
                            .content(cuerpo(datos)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(usuarios.findByEmailIgnoreCase("ana@test.com").orElseThrow().getTema())
                .isEqualTo(com.lossherpa.domain.Tema.SISTEMA);
    }

    @Test
    @DisplayName("RF7: las preferencias exigen sesion y CSRF")
    void preferenciasExigenSesionYCsrf() throws Exception {
        mvc.perform(put("/api/perfil/preferencias").with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"tema\":\"OSCURO\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/perfil/preferencias").session(iniciarSesion("ana@test.com"))
                        .contentType(APPLICATION_JSON).content("{\"tema\":\"OSCURO\"}"))
                .andExpect(status().isForbidden());
    }

    // --- RF7: telefono ---

    private void guardarTelefono(MockHttpSession sesion, Map<String, Object> datos, String tel,
                                 int estado) throws Exception {
        datos.put("telefono", tel);
        mvc.perform(put("/api/perfil").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().is(estado));
    }

    @Test
    @DisplayName("RF7: el telefono se guarda, se lee y se borra")
    void telefonoGuardarLeerBorrar() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        guardarTelefono(sesion, perfilAtleta(), "+54 9 11 5555-1234", 200);
        mvc.perform(get("/api/perfil").session(sesion))
                .andExpect(jsonPath("$.telefono").value("+54 9 11 5555-1234"));

        guardarTelefono(sesion, perfilAtleta(), null, 200);
        mvc.perform(get("/api/perfil").session(sesion))
                .andExpect(jsonPath("$.telefono").doesNotExist());
        assertThat(usuarios.findByEmailIgnoreCase("ana@test.com").orElseThrow().getTelefono())
                .isNull();
    }

    @Test
    @DisplayName("RF7: un telefono con formato invalido se rechaza")
    void telefonoInvalido() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        guardarTelefono(sesion, perfilAtleta(), "<script>alert(1)</script>", 400);
        guardarTelefono(sesion, perfilAtleta(), "abc", 400);
        guardarTelefono(sesion, perfilAtleta(), "1".repeat(31), 400);
    }

    @Test
    @DisplayName("RF7/RS06: el telefono lo ven las dos puntas del vinculo vigente y nadie mas")
    void visibilidadDelTelefono() throws Exception {
        var ana = usuarios.findByEmailIgnoreCase("ana@test.com").orElseThrow();
        var beto = usuarios.findByEmailIgnoreCase("beto@test.com").orElseThrow();
        ana.setTelefono("+5491155551111");
        beto.setTelefono("+5491155552222");
        usuarios.save(ana);
        usuarios.save(beto);
        var otro = crearEntrenador("otro@test.com", "Otro", "Coach");

        // Sin vinculo: ni la busqueda ni el detalle lo devuelven.
        MockHttpSession sesionAna = iniciarSesion("ana@test.com");
        String busqueda = mvc.perform(get("/api/atleta/entrenadores").session(sesionAna))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(busqueda).doesNotContain("telefono", "5555");
        mvc.perform(get("/api/entrenador/atletas/" + ana.getId())
                        .session(iniciarSesion("otro@test.com")))
                .andExpect(status().isNotFound());

        // Solicitud pendiente: tampoco.
        crearSolicitudPendiente(ana, otro);
        String pendientes = mvc.perform(get("/api/entrenador/solicitudes")
                        .session(iniciarSesion("otro@test.com")))
                .andReturn().getResponse().getContentAsString();
        assertThat(pendientes).doesNotContain("telefono", "5555");

        // Con vinculo vigente: cada uno ve el del otro.
        crearVinculoActivo(beto, ana);
        mvc.perform(get("/api/entrenador/atletas/" + ana.getId())
                        .session(iniciarSesion("beto@test.com")))
                .andExpect(jsonPath("$.telefono").value("+5491155551111"));
        mvc.perform(get("/api/entrenador/atletas").session(iniciarSesion("beto@test.com")))
                .andExpect(jsonPath("$[0].atleta.telefono").value("+5491155551111"));
        mvc.perform(get("/api/atleta/entrenador").session(sesionAna))
                .andExpect(jsonPath("$.entrenador.telefono").value("+5491155552222"));

        // El entrenador sin vinculo sigue sin verlo.
        mvc.perform(get("/api/entrenador/atletas/" + ana.getId())
                        .session(iniciarSesion("otro@test.com")))
                .andExpect(status().isNotFound());
    }
}
