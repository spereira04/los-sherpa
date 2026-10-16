package com.lossherpa.security;

import com.lossherpa.domain.Rol;
import com.lossherpa.security.PoliticaAcceso.Acceso;
import com.lossherpa.security.PoliticaAcceso.Atributo;
import com.lossherpa.security.PoliticaAcceso.Regla;
import com.lossherpa.support.InspectorDeEndpoints;
import com.lossherpa.support.InspectorDeEndpoints.Endpoint;
import com.lossherpa.support.TestIntegracion;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RS16 - Toda request pasa por el control de acceso.
 *
 * Cruza los handlers REALES de la aplicacion (todos los HandlerMapping, no solo los
 * controladores) contra PoliticaAcceso, en los dos sentidos, y despues ejecuta la matriz
 * completa regla x sesion contra la app levantada:
 *
 *  - un endpoint sin regla rompe el build (aunque igual quedaria denegado por RS02);
 *  - una regla sin endpoint (huerfana) rompe el build: es una puerta declarada abierta que
 *    se abriria sola el dia que alguien agregue un handler con ese patron;
 *  - sin sesion 401, rol que no corresponde 403, rol que corresponde ni 401 ni 403.
 *
 * Los mensajes nombran metodo y path para que el que rompe el build sepa que agregar.
 */
class CoberturaControlAccesoTest extends TestIntegracion {

    @Autowired
    private ApplicationContext contexto;

    @Autowired
    private PoliticaAcceso politica;

    /** null = sin sesion. LinkedHashMap para que los mensajes salgan siempre en el mismo orden. */
    private final Map<String, MockHttpSession> sesiones = new LinkedHashMap<>();

    @BeforeEach
    void datos() throws Exception {
        crearAtleta("ana@test.com", "Ana", "Suarez");
        crearEntrenador("coach@test.com", "Pablo", "Paz");
        crearAdmin("admin@test.com");
        sesiones.put("sin sesion", null);
        sesiones.put(Rol.ATLETA.name(), iniciarSesion("ana@test.com"));
        sesiones.put(Rol.ENTRENADOR.name(), iniciarSesion("coach@test.com"));
        sesiones.put(Rol.ADMIN.name(), iniciarSesion("admin@test.com"));
    }

    // --- Cobertura estatica: handlers <-> politica ---

    @Test
    @DisplayName("RS16: el inspector ve todos los handlers reales, incluidos estaticos y /error")
    void seRecorrenTodosLosHandlers() {
        List<String> patrones = InspectorDeEndpoints.de(contexto).stream()
                .map(Endpoint::patron).toList();

        assertThat(patrones)
                .contains("/api/perfil", "/api/admin/usuarios/{idUsuario}", "/atleta/**",
                        "/assets/**", "/favicon.ico", "/error", "/");
        // Cada controlador de la app aporta al menos un endpoint.
        assertThat(InspectorDeEndpoints.de(contexto).stream()
                .filter(e -> e.handlerMethod() != null)
                .map(e -> e.handlerMethod().getBeanType().getSimpleName())
                .distinct())
                .contains("AdminController", "AtletaRutinasController",
                        "AtletaVinculacionController", "AuthController",
                        "EntrenadorAtletasController", "EntrenadorRutinasController",
                        "PerfilController", "ControladorSpa", "BasicErrorController");
    }

    @Test
    @DisplayName("RS16: todo endpoint real tiene una regla explicita (metodo + patron)")
    void todoEndpointTieneRegla() {
        assertThat(endpointsSinRegla(InspectorDeEndpoints.de(contexto), politica)).isEmpty();
    }

    @Test
    @DisplayName("RS16: toda regla de la politica corresponde a un endpoint que existe")
    void ningunaReglaQuedaHuerfana() {
        assertThat(reglasHuerfanas(InspectorDeEndpoints.de(contexto), politica)).isEmpty();
    }

    @Test
    @DisplayName("RS16 (demostracion): un controlador nuevo sin regla hace fallar la verificacion")
    void unEndpointSinReglaSeDetecta() {
        List<Endpoint> conDebug = new ArrayList<>(InspectorDeEndpoints.de(contexto));
        conDebug.addAll(InspectorDeEndpoints.deControladores(ControladorDeDebug.class));

        assertThat(endpointsSinRegla(conDebug, politica))
                .singleElement().asString()
                .startsWith("GET /api/atleta/debug")
                .contains("ControladorDeDebug");
    }

    @Test
    @DisplayName("RS16 (demostracion): una regla sin endpoint hace fallar la verificacion")
    void unaReglaHuerfanaSeDetecta() {
        List<Regla> reglas = new ArrayList<>(politica.reglas());
        reglas.add(new Regla(HttpMethod.DELETE, "/api/atleta/ejecuciones/{idEjecucion}",
                Acceso.ATLETA, Atributo.DUENO));

        assertThat(reglasHuerfanas(InspectorDeEndpoints.de(contexto), new PoliticaAcceso(reglas)))
                .singleElement().asString()
                .startsWith("DELETE /api/atleta/ejecuciones/{idEjecucion}");
    }

    @Test
    @DisplayName("RS16 (demostracion): quitar una regla deja el endpoint sin cobertura")
    void quitarUnaReglaSeDetecta() {
        List<Regla> sinDesvincular = politica.reglas().stream()
                .filter(r -> !r.descripcion().equals("DELETE /api/atleta/vinculo"))
                .toList();

        assertThat(endpointsSinRegla(InspectorDeEndpoints.de(contexto),
                new PoliticaAcceso(sinDesvincular)))
                .singleElement().asString()
                .startsWith("DELETE /api/atleta/vinculo");
    }

    // --- Cobertura dinamica: la matriz contra la app levantada ---

    @Test
    @DisplayName("RS16: matriz completa regla x sesion: 401 sin sesion, 403 fuera de rol, acceso con el rol correcto")
    void matrizCompletaDeAcceso() throws Exception {
        List<String> fallas = new ArrayList<>();
        for (Regla regla : politica.reglas()) {
            for (HttpMethod metodo : metodosAProbar(regla)) {
                for (Map.Entry<String, MockHttpSession> sesion : sesiones.entrySet()) {
                    int estado = ejecutar(metodo, uriDePrueba(regla.patron()), sesion.getValue());
                    String esperado = esperado(regla, sesion.getKey());
                    if (!cumple(esperado, estado)) {
                        fallas.add(metodo.name() + " " + regla.patron() + " [" + sesion.getKey()
                                + "]: esperado " + esperado + ", fue " + estado);
                    }
                }
            }
        }
        assertThat(fallas).isEmpty();
    }

    // --- Caminos no obvios ---

    @Test
    @DisplayName("RS16: el forward de error del contenedor a /error tambien pasa por la politica y no entrega datos")
    void elForwardAErrorPasaPorLaPolitica() throws Exception {
        for (MockHttpSession sesion : sesiones.values()) {
            MockHttpServletRequestBuilder forwardDeError = get("/error").with(req -> {
                req.setDispatcherType(DispatcherType.ERROR);
                req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
                req.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/perfil");
                return req;
            });
            if (sesion != null) {
                forwardDeError.session(sesion);
            }
            String cuerpo = mvc.perform(forwardDeError)
                    .andExpect(status().isForbidden())
                    .andReturn().getResponse().getContentAsString();
            assertThat(cuerpo).doesNotContain("ana@test.com", "trace", "exception", "path");
        }
    }

    @Test
    @DisplayName("RS16: las rutas del SPA entregan el mismo index para todos, sin datos de usuario")
    void lasRutasDelSpaNoEntreganDatos() throws Exception {
        String anonimo = mvc.perform(get("/atleta/rutinas")).andReturn().getResponse()
                .getContentAsString();
        for (MockHttpSession sesion : sesiones.values()) {
            if (sesion == null) {
                continue;
            }
            MvcResult conSesion = mvc.perform(get("/atleta/rutinas").session(sesion))
                    .andExpect(status().isOk())
                    .andReturn();
            assertThat(conSesion.getResponse().getContentAsString()).isEqualTo(anonimo);
            assertThat(conSesion.getResponse().getContentType()).startsWith("text/html");
        }
    }

    // --- Verificacion ---

    /** Endpoints reales que no tienen una regla para cada uno de sus metodos. */
    static List<String> endpointsSinRegla(List<Endpoint> endpoints, PoliticaAcceso politica) {
        List<String> faltantes = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            String handler = endpoint.handlerMethod() != null
                    ? endpoint.handlerMethod().getShortLogMessage()
                    : endpoint.handler().getClass().getSimpleName();
            if (endpoint.aceptaCualquierMetodo()) {
                boolean cubierto = politica.reglas().stream().anyMatch(r ->
                        r.metodo() == null && r.patron().equals(endpoint.patron()));
                if (!cubierto) {
                    faltantes.add("* " + endpoint.patron() + ": acepta cualquier metodo y no "
                            + "tiene una regla para todos los metodos (" + handler + ")");
                }
                continue;
            }
            for (HttpMethod metodo : endpoint.metodos()) {
                if (politica.buscar(metodo, endpoint.patron()).isEmpty()) {
                    faltantes.add(metodo.name() + " " + endpoint.patron()
                            + ": endpoint sin regla en PoliticaAcceso (" + handler + ")");
                }
            }
        }
        return faltantes;
    }

    /** Reglas que no corresponden a ningun handler real. */
    static List<String> reglasHuerfanas(List<Endpoint> endpoints, PoliticaAcceso politica) {
        List<String> huerfanas = new ArrayList<>();
        for (Regla regla : politica.reglas()) {
            boolean existe = endpoints.stream().anyMatch(e ->
                    e.patron().equals(regla.patron())
                            && (regla.metodo() == null ? e.aceptaCualquierMetodo()
                            : e.aceptaCualquierMetodo() || e.metodos().contains(regla.metodo())));
            if (!existe) {
                huerfanas.add(regla.descripcion() + ": regla sin endpoint (huerfana)");
            }
        }
        return huerfanas;
    }

    // --- Matriz ---

    private static List<HttpMethod> metodosAProbar(Regla regla) {
        return regla.metodo() != null ? List.of(regla.metodo())
                : List.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH,
                HttpMethod.DELETE);
    }

    /** "401", "403" o "acceso" (ni 401 ni 403: 200, 400 o 404 por datos ficticios). */
    private static String esperado(Regla regla, String sesion) {
        boolean sinSesion = sesion.equals("sin sesion");
        return switch (regla.acceso()) {
            case PUBLICO -> "acceso";
            case DENEGADO -> "403";
            case AUTENTICADO -> sinSesion ? "401" : "acceso";
            case ATLETA, ENTRENADOR, ADMIN -> sinSesion ? "401"
                    : regla.acceso().name().equals(sesion) ? "acceso" : "403";
        };
    }

    private static boolean cumple(String esperado, int estado) {
        return switch (esperado) {
            case "401" -> estado == 401;
            case "403" -> estado == 403;
            default -> estado != 401 && estado != 403 && estado < 500;
        };
    }

    /** Variables de path con un UUID que no existe; comodines con un segmento cualquiera. */
    private static String uriDePrueba(String patron) {
        return patron.replaceAll("\\{[^}]+}", UUID.randomUUID().toString())
                .replace("/**", "/cualquier-ruta");
    }

    private int ejecutar(HttpMethod metodo, String uri, MockHttpSession sesion) throws Exception {
        MockHttpServletRequestBuilder req = request(metodo, uri);
        if (metodo != HttpMethod.GET) {
            req.with(csrf()).contentType(APPLICATION_JSON).content("{}");
        }
        if (sesion != null) {
            req.session(sesion);
        }
        return mvc.perform(req).andReturn().getResponse().getStatus();
    }

    /** El controlador de debug que alguien podria olvidarse adentro. Nunca se registra en la app. */
    @RestController
    static class ControladorDeDebug {

        @GetMapping("/api/atleta/debug")
        String debug() {
            return "estado interno";
        }
    }
}
