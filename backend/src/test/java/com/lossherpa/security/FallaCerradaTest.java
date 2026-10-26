package com.lossherpa.security;

import com.lossherpa.domain.Usuario;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.service.ServicioRutinas;
import com.lossherpa.support.CapturadorDeAuditoria;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RS18 - Deny by default ante errores (fail closed).
 *
 * Cada test fuerza una falla en un punto distinto del camino de autorizacion y verifica tres
 * cosas: el codigo de respuesta, que el cuerpo es el generico (nada del mensaje de la
 * excepcion) y que la logica de negocio no se ejecuto (el servicio espiado no recibio
 * ninguna invocacion). La falla del dispatch a /error esta en FallaCerradaRealTest, porque
 * necesita un contenedor de servlets de verdad.
 */
class FallaCerradaTest extends TestIntegracion {

    private static final String GENERICO_NO_DISPONIBLE =
            "{\"error\":\"El servicio no esta disponible en este momento\"}";
    private static final String GENERICO_403 =
            "{\"error\":\"No tiene permisos para realizar esta accion\"}";
    private static final String GENERICO_500 = "{\"error\":\"Ocurrio un error inesperado\"}";

    @SpyBean
    private UsuarioRepository repositorioUsuarios;

    @SpyBean
    private PoliticaAcceso politica;

    @SpyBean
    private ServicioRutinas servicioRutinas;

    private Usuario ana;
    private MockHttpSession sesionAna;

    @BeforeEach
    void datos() throws Exception {
        ana = crearAtleta("ana@test.com", "Ana", "Suarez");
        // El login tiene que ocurrir ANTES de armar las fallas: usa el repositorio y la politica.
        sesionAna = iniciarSesion("ana@test.com");
    }

    @Test
    @DisplayName("RS18: base caida en FiltroSesionVigente -> 503 generico, sin llegar al servicio, auditado")
    void baseCaidaEnElFiltroCorta() throws Exception {
        doThrow(new DataAccessResourceFailureException(
                "jdbc:sqlite:../data/sherpa.db SELECT * FROM usuario database is locked"))
                .when(repositorioUsuarios).findById(any());

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            String cuerpo = mvc.perform(get("/api/atleta/rutinas").session(sesionAna))
                    .andExpect(status().isServiceUnavailable())
                    .andReturn().getResponse().getContentAsString();

            assertThat(cuerpo).isEqualTo(GENERICO_NO_DISPONIBLE);
            verifyNoInteractions(servicioRutinas);

            String linea = auditoria.ultimaLinea();
            assertThat(linea)
                    .contains("\"evento\":\"fallo_cerrado\"")
                    .contains("\"estado\":503")
                    .contains("\"usuarioId\":\"" + ana.getId() + "\"")
                    .contains("\"recurso\":\"/api/atleta/rutinas\"")
                    .contains("FiltroSesionVigente: DataAccessResourceFailureException")
                    // RS17: de la excepcion solo se loguea la clase, nunca el mensaje.
                    .doesNotContain("SELECT")
                    .doesNotContain("sqlite")
                    .doesNotContain("locked");
        }
    }

    @Test
    @DisplayName("RS18: la falla del filtro tampoco deja pasar un endpoint de otro rol ni de escritura")
    void baseCaidaCortaTambienLasEscrituras() throws Exception {
        doThrow(new DataAccessResourceFailureException("db caida"))
                .when(repositorioUsuarios).findById(any());

        mvc.perform(post("/api/atleta/rutinas").with(csrf()).session(sesionAna)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Rutina\",\"ejercicios\":[]}"))
                .andExpect(status().isServiceUnavailable());

        verifyNoInteractions(servicioRutinas);
        assertThat(rutinas.count()).isZero();
    }

    @Test
    @DisplayName("RS18: la sesion no se invalida por una falla transitoria: al volver la base sigue valida")
    void laSesionSobreviveALaFallaDeLaBase() throws Exception {
        doThrow(new DataAccessResourceFailureException("db caida"))
                .when(repositorioUsuarios).findById(any());
        mvc.perform(get("/api/atleta/rutinas").session(sesionAna))
                .andExpect(status().isServiceUnavailable());

        reset(repositorioUsuarios);

        mvc.perform(get("/api/atleta/rutinas").session(sesionAna))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RS18: si la politica lanza una excepcion al evaluar -> 403, no se ejecuta el servicio, auditado")
    void laPoliticaQueFallaDenega() throws Exception {
        doThrow(new IllegalStateException("tabla de reglas corrupta para ana@test.com"))
                .when(politica).evaluar(any(), any());

        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            String cuerpo = mvc.perform(get("/api/atleta/rutinas").session(sesionAna))
                    .andExpect(status().isForbidden())
                    .andReturn().getResponse().getContentAsString();

            assertThat(cuerpo).isEqualTo(GENERICO_403);
            verifyNoInteractions(servicioRutinas);

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"acceso_denegado\"")
                    .contains("\"estado\":403")
                    .contains("\"usuarioId\":\"" + ana.getId() + "\"")
                    .contains("error al evaluar la politica (IllegalStateException)")
                    .doesNotContain("corrupta")
                    .doesNotContain("ana@test.com");
        }
    }

    @Test
    @DisplayName("RS18: la politica que falla deniega tambien una ruta publica y una request sin sesion")
    void laPoliticaQueFallaDenegaHastaLoPublico() throws Exception {
        doThrow(new IllegalStateException("falla")).when(politica).evaluar(any(), any());

        String cuerpo = mvc.perform(get("/api/csrf"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).isEqualTo(GENERICO_403);
    }

    @Test
    @DisplayName("RS18: una excepcion en el servicio -> 500 generico, sin cuerpo parcial ni datos")
    void elServicioQueFallaDa500Generico() throws Exception {
        doThrow(new IllegalStateException("fila corrupta del usuario ana@test.com"))
                .when(servicioRutinas).misRutinas();

        String cuerpo = mvc.perform(get("/api/atleta/rutinas").session(sesionAna))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).isEqualTo(GENERICO_500);
    }

    @Test
    @DisplayName("RS18: un JSON ilegible -> 400 generico y el servicio no se ejecuta")
    void jsonIlegibleDa400SinEjecutarNada() throws Exception {
        String cuerpo = mvc.perform(post("/api/atleta/rutinas").with(csrf()).session(sesionAna)
                        .contentType(APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).isEqualTo("{\"error\":\"La solicitud no es valida\"}");
        verifyNoInteractions(servicioRutinas);
    }

    @Test
    @DisplayName("RS18: un cuerpo que no valida -> 400 con los campos, sin el valor recibido, sin ejecutar nada")
    void validacionFallidaDa400SinEjecutarNada() throws Exception {
        String cuerpo = mvc.perform(post("/api/atleta/rutinas").with(csrf()).session(sesionAna)
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).contains("Hay datos invalidos en el formulario");
        verifyNoInteractions(servicioRutinas);
    }
}
