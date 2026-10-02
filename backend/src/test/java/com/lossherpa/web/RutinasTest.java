package com.lossherpa.web;

import com.lossherpa.domain.OrigenRutina;
import com.lossherpa.domain.Rutina;
import com.lossherpa.domain.Usuario;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RS15: rutinas, con foco en IDOR y en las reglas de visibilidad del entrenador. */
class RutinasTest extends TestIntegracion {

    private Usuario atleta;
    private Usuario otroAtleta;
    private Usuario entrenador;
    private Usuario otroEntrenador;

    @BeforeEach
    void datos() {
        atleta = crearAtleta("ana@test.com", "Ana", "Suarez");
        otroAtleta = crearAtleta("caro@test.com", "Caro", "Diaz");
        entrenador = crearEntrenador("beto@test.com", "Beto", "Lopez");
        otroEntrenador = crearEntrenador("dario@test.com", "Dario", "Paz");
    }

    private Map<String, Object> cuerpoRutina(String nombre) {
        return Map.of("nombre", nombre, "ejercicios", List.of(
                Map.of("nombre", "Banco plano", "series", 3, "repeticiones", 10),
                Map.of("nombre", "Remo", "series", 2, "repeticiones", 12)));
    }

    // -------------------------------------------------------------- atleta: propias

    @Test
    @DisplayName("el atleta crea su rutina propia y queda con origen PROPIA")
    void creaRutinaPropia() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(post("/api/atleta/rutinas").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(cuerpoRutina("Mi full body"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("PROPIA"))
                .andExpect(jsonPath("$.creadaPor").doesNotExist())
                .andExpect(jsonPath("$.ejercicios.length()").value(2))
                .andExpect(jsonPath("$.ejercicios[0].orden").value(1));

        assertThat(rutinas.listarPorAtleta(atleta.getId()))
                .singleElement()
                .satisfies(r -> assertThat(r.getOrigen()).isEqualTo(OrigenRutina.PROPIA));
    }

    @Test
    @DisplayName("el atleta no puede forzar el origen ni el dueno de su rutina")
    void noPuedeForzarOrigenNiDueno() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(post("/api/atleta/rutinas").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of(
                                "nombre", "Trucha",
                                "origen", "ASIGNADA",
                                "idAtleta", otroAtleta.getId().toString(),
                                "ejercicios", List.of(Map.of(
                                        "nombre", "Banco", "series", 1, "repeticiones", 5))))))
                .andExpect(status().isBadRequest());

        assertThat(rutinas.count()).isZero();
    }

    @Test
    @DisplayName("una rutina sin ejercicios se rechaza")
    void rutinaSinEjercicios() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(post("/api/atleta/rutinas").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("nombre", "Vacia", "ejercicios", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("el atleta ve sus rutinas asignadas y propias juntas")
    void veAsignadasYPropias() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        crearRutinaAsignada(entrenador, atleta, "Fuerza 1");
        crearRutinaPropia(atleta, "Cardio propio");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/atleta/rutinas").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("IDOR: el atleta no puede leer la rutina de otro atleta")
    void noLeeRutinaAjena() throws Exception {
        Rutina ajena = crearRutinaPropia(otroAtleta, "Rutina de Caro");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/atleta/rutinas/" + ajena.getId()).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("IDOR: el atleta no puede completar la rutina de otro atleta")
    void noCompletaRutinaAjena() throws Exception {
        Rutina ajena = crearRutinaPropia(otroAtleta, "Rutina de Caro");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(post("/api/atleta/rutinas/" + ajena.getId() + "/ejecuciones")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(cuerpoEjecucion(ajena, java.time.LocalDate.now(), 40))))
                .andExpect(status().isNotFound());

        assertThat(ejecuciones.count()).isZero();
    }

    @Test
    @DisplayName("una rutina inexistente da 404, igual que una ajena")
    void rutinaInexistente() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/atleta/rutinas/" + UUID.randomUUID()).session(sesion))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------- entrenador: asignar

    @Test
    @DisplayName("el entrenador crea una rutina para su atleta vinculado")
    void creaRutinaParaSuAtleta() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(post("/api/entrenador/atletas/" + atleta.getId() + "/rutinas")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(cuerpoRutina("Fuerza semana 1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("ASIGNADA"))
                .andExpect(jsonPath("$.creadaPor").value("Beto Lopez"));

        assertThat(rutinas.listarPorAtleta(atleta.getId())).hasSize(1);
    }

    @Test
    @DisplayName("el entrenador no puede crear una rutina para un atleta que no es suyo")
    void noCreaRutinaParaAtletaAjeno() throws Exception {
        crearVinculoActivo(otroEntrenador, otroAtleta);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(post("/api/entrenador/atletas/" + otroAtleta.getId() + "/rutinas")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(cuerpoRutina("Intrusa"))))
                .andExpect(status().isNotFound());

        assertThat(rutinas.count()).isZero();
    }

    @Test
    @DisplayName("el entrenador no puede crear una rutina sin vinculo activo")
    void noCreaRutinaSinVinculo() throws Exception {
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(post("/api/entrenador/atletas/" + atleta.getId() + "/rutinas")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(cuerpoRutina("Sin vinculo"))))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------- entrenador: visibilidad

    @Test
    @DisplayName("el entrenador ve las rutinas que el asigno")
    void veLasRutinasQueAsigno() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        crearRutinaAsignada(entrenador, atleta, "Fuerza 1");
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId() + "/rutinas").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Fuerza 1"));
    }

    @Test
    @DisplayName("el entrenador NUNCA ve las rutinas propias de su atleta")
    void noVeLasRutinasPropiasDelAtleta() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        crearRutinaAsignada(entrenador, atleta, "Fuerza 1");
        Rutina propia = crearRutinaPropia(atleta, "Cardio secreto");
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        String respuesta = mvc.perform(
                        get("/api/entrenador/atletas/" + atleta.getId() + "/rutinas")
                                .session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta)
                .doesNotContain("Cardio secreto")
                .doesNotContain(propia.getId().toString());
    }

    @Test
    @DisplayName("el entrenador no ve las rutinas que asigno OTRO entrenador")
    void noVeLasRutinasDeOtroEntrenador() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        // Rutina historica, asignada por otro entrenador cuando lo entrenaba el.
        crearRutinaAsignada(otroEntrenador, atleta, "Vieja de Dario");
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId() + "/rutinas").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("IDOR: el entrenador no ve las rutinas de un atleta que no es suyo")
    void noVeRutinasDeAtletaAjeno() throws Exception {
        crearVinculoActivo(otroEntrenador, otroAtleta);
        crearRutinaAsignada(otroEntrenador, otroAtleta, "De Caro");
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + otroAtleta.getId() + "/rutinas")
                        .session(sesion))
                .andExpect(status().isNotFound());
    }

    // --------------------------------------------------------------- RS10

    @Test
    @DisplayName("RS10: al desvincular, el entrenador pierde acceso a las rutinas")
    void alDesvincularPierdeAccesoALasRutinas() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        crearRutinaAsignada(entrenador, atleta, "Fuerza 1");
        MockHttpSession sesionEntrenador = iniciarSesion("beto@test.com");
        MockHttpSession sesionAtleta = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId() + "/rutinas")
                        .session(sesionEntrenador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(delete("/api/atleta/vinculo").with(csrf()).session(sesionAtleta))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId() + "/rutinas")
                        .session(sesionEntrenador))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el atleta conserva las rutinas asignadas despues de desvincularse")
    void elAtletaConservaSusRutinasAsignadas() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        crearRutinaAsignada(entrenador, atleta, "Fuerza 1");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(delete("/api/atleta/vinculo").with(csrf()).session(sesion))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/atleta/rutinas").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Fuerza 1"))
                .andExpect(jsonPath("$[0].creadaPor").value("Beto Lopez"));
    }
}
