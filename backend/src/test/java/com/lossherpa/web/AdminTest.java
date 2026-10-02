package com.lossherpa.web;

import com.lossherpa.domain.OrigenRutina;
import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Rutina;
import com.lossherpa.domain.Usuario;
import com.lossherpa.support.CapturadorDeAuditoria;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RS15: panel de admin, imposibilidad de crear admins y RS10 al eliminar un usuario. */
class AdminTest extends TestIntegracion {

    private Usuario admin;
    private Usuario atleta;
    private Usuario entrenador;

    @BeforeEach
    void datos() {
        admin = crearAdmin("admin@test.com");
        atleta = crearAtleta("ana@test.com", "Ana", "Suarez");
        entrenador = crearEntrenador("beto@test.com", "Beto", "Lopez");
    }

    private Map<String, Object> altaAtleta() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("email", "nuevo@test.com");
        datos.put("contrasena", PASSWORD);
        datos.put("rol", "ATLETA");
        datos.put("nombre", "Nuevo");
        datos.put("apellido", "Atleta");
        datos.put("fechaNacimiento", "2001-04-04");
        datos.put("pesoKg", 68.0);
        return datos;
    }

    // -------------------------------------------------------------- acceso

    @Test
    @DisplayName("sin sesion el panel de admin devuelve 401")
    void sinSesion401() throws Exception {
        mvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un entrenador no puede entrar al panel de admin")
    void entrenadorNoEntra() throws Exception {
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/admin/usuarios").session(sesion))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("el admin no puede entrar a los endpoints de atleta ni de entrenador")
    void elAdminNoUsaLosOtrosPaneles() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(get("/api/atleta/rutinas").session(sesion))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/entrenador/atletas").session(sesion))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------- listado

    @Test
    @DisplayName("el listado trae entrenadores y atletas, nunca otros admins")
    void elListadoNoTraeAdmins() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        String respuesta = mvc.perform(get("/api/admin/usuarios").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta)
                .doesNotContain("admin@test.com")
                .doesNotContain("ADMIN")
                .doesNotContain("passwordHash");
    }

    @Test
    @DisplayName("el listado filtra por rol y por texto")
    void elListadoFiltra() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(get("/api/admin/usuarios").param("rol", "ATLETA").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("ana@test.com"));

        mvc.perform(get("/api/admin/usuarios").param("q", "lopez").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].apellido").value("Lopez"));
    }

    @Test
    @DisplayName("pedir el listado de admins no devuelve nada")
    void noSePuedenListarAdmins() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(get("/api/admin/usuarios").param("rol", "ADMIN").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("el admin no puede ver a otro admin por id")
    void noVeAOtroAdmin() throws Exception {
        Usuario otroAdmin = crearAdmin("admin2@test.com");
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(get("/api/admin/usuarios/" + otroAdmin.getId()).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el admin no puede verse a si mismo por el panel")
    void noSeVeASiMismo() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(get("/api/admin/usuarios/" + admin.getId()).session(sesion))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------------- alta

    @Test
    @DisplayName("el admin crea un atleta")
    void creaUnAtleta() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(post("/api/admin/usuarios").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(altaAtleta())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ATLETA"));

        assertThat(usuarios.findByEmailIgnoreCase("nuevo@test.com")).isPresent();
    }

    @Test
    @DisplayName("el admin NO puede crear otro admin")
    void noPuedeCrearOtroAdmin() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");
        Map<String, Object> datos = altaAtleta();
        datos.put("rol", "ADMIN");

        mvc.perform(post("/api/admin/usuarios").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());

        assertThat(usuarios.findAll().stream().filter(u -> u.getRol() == Rol.ADMIN)).hasSize(1);
    }

    @Test
    @DisplayName("el alta aplica la politica de contrasenas")
    void elAltaAplicaLaPolitica() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");
        Map<String, Object> datos = altaAtleta();
        datos.put("contrasena", "corta");

        mvc.perform(post("/api/admin/usuarios").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());
    }

    // --------------------------------------------------------- modificacion

    @Test
    @DisplayName("el admin modifica nombre y apellido")
    void modificaUnUsuario() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(put("/api/admin/usuarios/" + atleta.getId()).with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of(
                                "nombre", "Ana Laura",
                                "apellido", "Suarez",
                                "fechaNacimiento", "1998-03-12",
                                "pesoKg", 71.0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Laura"));

        assertThat(usuarios.findById(atleta.getId()).orElseThrow().getNombre())
                .isEqualTo("Ana Laura");
    }

    @Test
    @DisplayName("el admin tampoco puede cambiar el rol ni el email de un usuario")
    void noPuedeCambiarRolNiEmail() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(put("/api/admin/usuarios/" + atleta.getId()).with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of(
                                "nombre", "Ana",
                                "apellido", "Suarez",
                                "fechaNacimiento", "1998-03-12",
                                "pesoKg", 71.0,
                                "rol", "ADMIN",
                                "email", "otro@test.com"))))
                .andExpect(status().isBadRequest());

        Usuario sinCambios = usuarios.findById(atleta.getId()).orElseThrow();
        assertThat(sinCambios.getRol()).isEqualTo(Rol.ATLETA);
        assertThat(sinCambios.getEmail()).isEqualTo("ana@test.com");
    }

    // ----------------------------------------------------------------- baja

    @Test
    @DisplayName("RS10: el usuario eliminado queda con la sesion invalidada en el request siguiente")
    void elUsuarioEliminadoPierdeLaSesion() throws Exception {
        MockHttpSession sesionAtleta = iniciarSesion("ana@test.com");
        MockHttpSession sesionAdmin = iniciarSesion("admin@test.com");

        // Con la sesion abierta, el atleta accede normalmente.
        mvc.perform(get("/api/perfil").session(sesionAtleta)).andExpect(status().isOk());

        mvc.perform(delete("/api/admin/usuarios/" + atleta.getId())
                        .with(csrf()).session(sesionAdmin))
                .andExpect(status().isNoContent());

        // Misma sesion, request siguiente: 401.
        mvc.perform(get("/api/perfil").session(sesionAtleta))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("La sesion ya no es valida"));
    }

    @Test
    @DisplayName("RS12: la sesion invalidada deja una entrada de auditoria sin datos sensibles")
    void laSesionInvalidadaSeAudita() throws Exception {
        MockHttpSession sesionAtleta = iniciarSesion("ana@test.com");
        MockHttpSession sesionAdmin = iniciarSesion("admin@test.com");

        mvc.perform(delete("/api/admin/usuarios/" + atleta.getId())
                        .with(csrf()).session(sesionAdmin))
                .andExpect(status().isNoContent());

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(get("/api/perfil").session(sesionAtleta))
                    .andExpect(status().isUnauthorized());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"sesion_invalidada\"")
                    .contains(atleta.getId().toString())
                    .doesNotContain("ana@test.com")
                    .doesNotContain(PASSWORD)
                    .doesNotContain("SHERPASESSION");
        }
    }

    @Test
    @DisplayName("al eliminar un atleta se borran sus rutinas y ejecuciones")
    void alEliminarUnAtletaSeBorraSuData() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        Rutina asignada = crearRutinaAsignada(entrenador, atleta, "Fuerza 1");
        crearEjecucion(asignada, LocalDate.now(), 60);
        crearRutinaPropia(atleta, "Cardio");
        crearSolicitudPendiente(atleta, entrenador);

        MockHttpSession sesionAdmin = iniciarSesion("admin@test.com");
        mvc.perform(delete("/api/admin/usuarios/" + atleta.getId())
                        .with(csrf()).session(sesionAdmin))
                .andExpect(status().isNoContent());

        assertThat(usuarios.findById(atleta.getId())).isEmpty();
        assertThat(rutinas.count()).isZero();
        assertThat(ejecuciones.count()).isZero();
        assertThat(vinculos.count()).isZero();
        assertThat(solicitudes.count()).isZero();
    }

    @Test
    @DisplayName("al eliminar un entrenador, el atleta conserva las rutinas que le asigno")
    void alEliminarUnEntrenadorElAtletaConservaSusRutinas() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        crearRutinaAsignada(entrenador, atleta, "Fuerza 1");

        MockHttpSession sesionAdmin = iniciarSesion("admin@test.com");
        mvc.perform(delete("/api/admin/usuarios/" + entrenador.getId())
                        .with(csrf()).session(sesionAdmin))
                .andExpect(status().isNoContent());

        assertThat(usuarios.findById(entrenador.getId())).isEmpty();

        // La rutina sigue siendo del atleta, ya sin creador.
        MockHttpSession sesionAtleta = iniciarSesion("ana@test.com");
        mvc.perform(get("/api/atleta/rutinas").session(sesionAtleta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Fuerza 1"))
                .andExpect(jsonPath("$[0].creadaPor").doesNotExist());

        assertThat(rutinas.listarPorAtleta(atleta.getId()))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getOrigen()).isEqualTo(OrigenRutina.ASIGNADA);
                    assertThat(r.getEntrenadorCreador()).isNull();
                });
    }

    @Test
    @DisplayName("el admin no puede eliminar a otro admin ni a si mismo")
    void noPuedeEliminarAdmins() throws Exception {
        Usuario otroAdmin = crearAdmin("admin2@test.com");
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(delete("/api/admin/usuarios/" + otroAdmin.getId())
                        .with(csrf()).session(sesion))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/usuarios/" + admin.getId())
                        .with(csrf()).session(sesion))
                .andExpect(status().isNotFound());

        assertThat(usuarios.findById(otroAdmin.getId())).isPresent();
        assertThat(usuarios.findById(admin.getId())).isPresent();
    }

    @Test
    @DisplayName("eliminar un id inexistente devuelve 404")
    void eliminarIdInexistente() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(delete("/api/admin/usuarios/" + UUID.randomUUID())
                        .with(csrf()).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("una baja sin token CSRF se rechaza")
    void bajaSinCsrf() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        mvc.perform(delete("/api/admin/usuarios/" + atleta.getId()).session(sesion))
                .andExpect(status().isForbidden());

        assertThat(usuarios.findById(atleta.getId())).isPresent();
    }
}
