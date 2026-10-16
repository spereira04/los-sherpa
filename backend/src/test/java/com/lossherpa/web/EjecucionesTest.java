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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RS15: ejecuciones, validacion de las series cargadas y visibilidad del historial. */
class EjecucionesTest extends TestIntegracion {

    private static final LocalDate HOY = LocalDate.now();

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

    // ------------------------------------------------------------------ registrar

    @Test
    @DisplayName("el atleta completa su rutina y se guarda la carga de cada serie")
    void completaSuRutina() throws Exception {
        Rutina rutina = crearRutinaPropia(atleta, "Espalda");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(post("/api/atleta/rutinas/" + rutina.getId() + "/ejecuciones")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(cuerpoEjecucion(rutina, HOY, 47.5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreRutina").value("Espalda"))
                .andExpect(jsonPath("$.series.length()").value(2))
                .andExpect(jsonPath("$.series[0].cargaKg").value(47.5))
                .andExpect(jsonPath("$.series[0].nombreEjercicio").value("Dominadas"));
    }

    @Test
    @DisplayName("la misma rutina se puede completar muchas veces")
    void seCompletaMuchasVeces() throws Exception {
        Rutina rutina = crearRutinaPropia(atleta, "Espalda");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        for (double carga : new double[] {40, 42.5, 45}) {
            mvc.perform(post("/api/atleta/rutinas/" + rutina.getId() + "/ejecuciones")
                            .with(csrf()).session(sesion)
                            .contentType(APPLICATION_JSON)
                            .content(cuerpo(cuerpoEjecucion(rutina, HOY, carga))))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/atleta/ejecuciones").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("faltar una serie invalida la ejecucion")
    void faltarUnaSerieEsInvalido() throws Exception {
        Rutina rutina = crearRutinaAsignadaConVinculo();
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        Map<String, Object> completo = cuerpoEjecucion(rutina, HOY, 50);
        List<?> todas = (List<?>) completo.get("series");
        Map<String, Object> incompleto = Map.of(
                "fecha", HOY.toString(),
                "series", todas.subList(0, todas.size() - 1));

        mvc.perform(post("/api/atleta/rutinas/" + rutina.getId() + "/ejecuciones")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(incompleto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("todas las series")));

        assertThat(ejecuciones.count()).isZero();
    }

    @Test
    @DisplayName("una serie de un ejercicio de otra rutina se rechaza")
    void serieDeOtraRutinaSeRechaza() throws Exception {
        Rutina propia = crearRutinaPropia(atleta, "Espalda");
        Rutina otra = crearRutinaPropia(otroAtleta, "Pierna de Caro");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        String idEjercicioAjeno = otra.getEjercicios().get(0).getId().toString();
        Map<String, Object> datos = Map.of(
                "fecha", HOY.toString(),
                "series", List.of(Map.of(
                        "idEjercicio", idEjercicioAjeno, "nroSerie", 1, "cargaKg", 30.0)));

        mvc.perform(post("/api/atleta/rutinas/" + propia.getId() + "/ejecuciones")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("no corresponde")));

        assertThat(ejecuciones.count()).isZero();
    }

    @Test
    @DisplayName("cargar dos veces la misma serie se rechaza")
    void serieDuplicadaSeRechaza() throws Exception {
        Rutina rutina = crearRutinaPropia(atleta, "Espalda");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        String idEjercicio = rutina.getEjercicios().get(0).getId().toString();
        List<Map<String, Object>> series = new ArrayList<>();
        series.add(Map.of("idEjercicio", idEjercicio, "nroSerie", 1, "cargaKg", 30.0));
        series.add(Map.of("idEjercicio", idEjercicio, "nroSerie", 1, "cargaKg", 35.0));

        mvc.perform(post("/api/atleta/rutinas/" + rutina.getId() + "/ejecuciones")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of("fecha", HOY.toString(), "series", series))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("dos veces")));
    }

    @Test
    @DisplayName("un numero de serie mayor al de la rutina se rechaza")
    void nroDeSerieFueraDeRango() throws Exception {
        Rutina rutina = crearRutinaPropia(atleta, "Espalda");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        String idEjercicio = rutina.getEjercicios().get(0).getId().toString();
        Map<String, Object> datos = Map.of(
                "fecha", HOY.toString(),
                "series", List.of(Map.of(
                        "idEjercicio", idEjercicio, "nroSerie", 99, "cargaKg", 30.0)));

        mvc.perform(post("/api/atleta/rutinas/" + rutina.getId() + "/ejecuciones")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("una fecha futura se rechaza")
    void fechaFuturaSeRechaza() throws Exception {
        Rutina rutina = crearRutinaPropia(atleta, "Espalda");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        Map<String, Object> datos = cuerpoEjecucion(rutina, HOY.plusDays(1), 30);

        mvc.perform(post("/api/atleta/rutinas/" + rutina.getId() + "/ejecuciones")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(datos)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("una carga negativa se rechaza")
    void cargaNegativaSeRechaza() throws Exception {
        Rutina rutina = crearRutinaPropia(atleta, "Espalda");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(post("/api/atleta/rutinas/" + rutina.getId() + "/ejecuciones")
                        .with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(cuerpoEjecucion(rutina, HOY, -5))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------- historial propio

    @Test
    @DisplayName("el historial del atleta incluye las ejecuciones de todas sus rutinas")
    void elHistorialIncluyeTodo() throws Exception {
        Rutina asignada = crearRutinaAsignadaConVinculo();
        Rutina propia = crearRutinaPropia(atleta, "Cardio");
        crearEjecucion(asignada, HOY, 60);
        crearEjecucion(propia, HOY, 20);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/atleta/ejecuciones").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("el historial se filtra por rango de fechas")
    void filtraPorRangoDeFechas() throws Exception {
        Rutina rutina = crearRutinaPropia(atleta, "Espalda");
        crearEjecucion(rutina, HOY.minusDays(30), 40);
        crearEjecucion(rutina, HOY.minusDays(2), 45);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/atleta/ejecuciones").session(sesion)
                        .param("desde", HOY.minusDays(7).toString())
                        .param("hasta", HOY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].series[0].cargaKg").value(45.0));
    }

    @Test
    @DisplayName("un rango invertido se rechaza")
    void rangoInvertidoSeRechaza() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/atleta/ejecuciones").session(sesion)
                        .param("desde", HOY.toString())
                        .param("hasta", HOY.minusDays(5).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IDOR: el historial de un atleta no incluye ejecuciones de otro")
    void elHistorialNoFiltraDeOtros() throws Exception {
        Rutina ajena = crearRutinaPropia(otroAtleta, "Pierna de Caro");
        crearEjecucion(ajena, HOY, 100);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/atleta/ejecuciones").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("RF4: el atleta elimina su ejecución y deja de aparecer para él y su entrenador")
    void eliminaSuEjecucion() throws Exception {
        Rutina asignada = crearRutinaAsignadaConVinculo();
        var ejecucion = crearEjecucion(asignada, HOY, 80);
        MockHttpSession sesionAtleta = iniciarSesion("ana@test.com");
        MockHttpSession sesionEntrenador = iniciarSesion("beto@test.com");

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(delete("/api/atleta/ejecuciones/" + ejecucion.getId())
                            .with(csrf()).session(sesionAtleta))
                    .andExpect(status().isNoContent());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"ejecucion_eliminada\"")
                    .contains(ejecucion.getId().toString())
                    .doesNotContain("80.0");
        }

        mvc.perform(get("/api/atleta/ejecuciones").session(sesionAtleta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId() + "/ejecuciones")
                        .session(sesionEntrenador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("RS04: un atleta no puede eliminar una ejecución ajena")
    void noEliminaEjecucionAjena() throws Exception {
        Rutina ajena = crearRutinaPropia(otroAtleta, "Rutina de Caro");
        var ejecucion = crearEjecucion(ajena, HOY, 45);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(delete("/api/atleta/ejecuciones/" + ejecucion.getId())
                            .with(csrf()).session(sesion))
                    .andExpect(status().isNotFound());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"acceso_denegado\"")
                    .contains("\"estado\":404");
        }

        assertThat(ejecuciones.existsById(ejecucion.getId())).isTrue();
    }

    @Test
    @DisplayName("RS08: el entrenador no puede borrar ejecuciones")
    void entrenadorNoPuedeEliminarEjecucion() throws Exception {
        Rutina asignada = crearRutinaAsignadaConVinculo();
        var ejecucion = crearEjecucion(asignada, HOY, 80);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            mvc.perform(delete("/api/atleta/ejecuciones/" + ejecucion.getId())
                            .with(csrf()).session(sesion))
                    .andExpect(status().isForbidden());

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"acceso_denegado\"")
                    .contains("\"estado\":403");
        }

        assertThat(ejecuciones.existsById(ejecucion.getId())).isTrue();
    }

    @Test
    @DisplayName("RF4 + RS04: borrar todo solo elimina las ejecuciones del atleta autenticado")
    void eliminaTodoSuHistorialSinAfectarAOtros() throws Exception {
        Rutina propia = crearRutinaPropia(atleta, "Espalda");
        Rutina ajena = crearRutinaPropia(otroAtleta, "Espalda de Caro");
        crearEjecucion(propia, HOY, 30);
        var ajenaEjecucion = crearEjecucion(ajena, HOY, 40);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(delete("/api/atleta/ejecuciones").with(csrf()).session(sesion))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/atleta/ejecuciones").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        assertThat(ejecuciones.existsById(ajenaEjecucion.getId())).isTrue();
    }

    // -------------------------------------------------- historial visto por el entrenador

    @Test
    @DisplayName("el entrenador ve las ejecuciones de las rutinas que el asigno")
    void veLasEjecucionesDeSusRutinas() throws Exception {
        Rutina asignada = crearRutinaAsignadaConVinculo();
        crearEjecucion(asignada, HOY, 80);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId() + "/ejecuciones")
                        .session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].series[0].cargaKg").value(80.0));
    }

    @Test
    @DisplayName("el entrenador NUNCA ve las ejecuciones de las rutinas propias del atleta")
    void noVeEjecucionesDeRutinasPropias() throws Exception {
        Rutina asignada = crearRutinaAsignadaConVinculo();
        Rutina propia = crearRutinaPropia(atleta, "Cardio secreto");
        crearEjecucion(asignada, HOY, 80);
        crearEjecucion(propia, HOY, 15);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        String respuesta = mvc.perform(
                        get("/api/entrenador/atletas/" + atleta.getId() + "/ejecuciones")
                                .session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta)
                .doesNotContain("Cardio secreto")
                .doesNotContain(propia.getId().toString());
    }

    @Test
    @DisplayName("el entrenador no ve las ejecuciones de rutinas asignadas por otro entrenador")
    void noVeEjecucionesDeOtroEntrenador() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        Rutina deOtro = crearRutinaAsignada(otroEntrenador, atleta, "Vieja de Dario");
        crearEjecucion(deOtro, HOY, 70);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId() + "/ejecuciones")
                        .session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("IDOR: el entrenador no ve el historial de un atleta que no es suyo")
    void noVeHistorialDeAtletaAjeno() throws Exception {
        crearVinculoActivo(otroEntrenador, otroAtleta);
        Rutina ajena = crearRutinaAsignada(otroEntrenador, otroAtleta, "De Caro");
        crearEjecucion(ajena, HOY, 90);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + otroAtleta.getId() + "/ejecuciones")
                        .session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RS10: al desvincular, el entrenador pierde acceso al historial")
    void alDesvincularPierdeAccesoAlHistorial() throws Exception {
        Rutina asignada = crearRutinaAsignadaConVinculo();
        crearEjecucion(asignada, HOY, 80);
        MockHttpSession sesionEntrenador = iniciarSesion("beto@test.com");
        MockHttpSession sesionAtleta = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId() + "/ejecuciones")
                        .session(sesionEntrenador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(delete("/api/atleta/vinculo").with(csrf()).session(sesionAtleta))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId() + "/ejecuciones")
                        .session(sesionEntrenador))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el entrenador tambien puede filtrar el historial por fechas")
    void elEntrenadorFiltraPorFechas() throws Exception {
        Rutina asignada = crearRutinaAsignadaConVinculo();
        crearEjecucion(asignada, HOY.minusDays(40), 70);
        crearEjecucion(asignada, HOY.minusDays(1), 75);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId() + "/ejecuciones")
                        .session(sesion)
                        .param("desde", HOY.minusDays(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private Rutina crearRutinaAsignadaConVinculo() {
        crearVinculoActivo(entrenador, atleta);
        return crearRutinaAsignada(entrenador, atleta, "Fuerza 1");
    }
}
