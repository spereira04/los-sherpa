package com.lossherpa.security;

import com.lossherpa.domain.Rol;
import com.lossherpa.support.CapturadorDeAuditoria;
import com.lossherpa.support.InspectorDeEndpoints;
import com.lossherpa.support.InspectorDeEndpoints.Endpoint;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RS01 + RS03 + RS05 + RS08: la politica central de autorizacion.
 *
 * Aca se fija el comportamiento por grupo de rol, con su linea de auditoria, y que ningun
 * handler pueda recibir el rol o la identidad del usuario actual desde el cliente.
 */
class PoliticaAccesoTest extends TestIntegracion {

    /** Dos endpoints representativos de cada grupo de rol. */
    private static final Map<Rol, List<String>> GRUPOS = Map.of(
            Rol.ATLETA, List.of("/api/atleta/rutinas", "/api/atleta/solicitudes"),
            Rol.ENTRENADOR, List.of("/api/entrenador/atletas",
                    "/api/entrenador/atletas/" + UUID.randomUUID() + "/rutinas"),
            Rol.ADMIN, List.of("/api/admin/usuarios",
                    "/api/admin/usuarios/" + UUID.randomUUID()));

    @Autowired
    private ApplicationContext contexto;

    @Autowired
    private PoliticaAcceso politica;

    private Map<Rol, MockHttpSession> sesiones;

    @BeforeEach
    void datos() throws Exception {
        crearAtleta("ana@test.com", "Ana", "Suarez");
        crearEntrenador("coach@test.com", "Pablo", "Paz");
        crearAdmin("admin@test.com");
        sesiones = Map.of(
                Rol.ATLETA, iniciarSesion("ana@test.com"),
                Rol.ENTRENADOR, iniciarSesion("coach@test.com"),
                Rol.ADMIN, iniciarSesion("admin@test.com"));
    }

    @Test
    @DisplayName("RS01: sin sesion, todo endpoint de rol responde 401")
    void sinSesionEs401() throws Exception {
        for (List<String> rutas : GRUPOS.values()) {
            for (String ruta : rutas) {
                mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            }
        }
    }

    @Test
    @DisplayName("RS03: cada rol fuera de su grupo recibe 403 y cada 403 deja una linea de auditoria")
    void rolEquivocadoEs403YSeAudita() throws Exception {
        for (Rol rol : Rol.values()) {
            for (Map.Entry<Rol, List<String>> grupo : GRUPOS.entrySet()) {
                if (grupo.getKey() == rol) {
                    continue;
                }
                for (String ruta : grupo.getValue()) {
                    try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
                        mvc.perform(get(ruta).session(sesiones.get(rol)))
                                .andExpect(status().isForbidden());

                        assertThat(auditoria.lineas())
                                .as("%s en %s", rol, ruta)
                                .hasSize(1);
                        assertThat(auditoria.ultimaLinea())
                                .contains("\"estado\":403")
                                .contains("el endpoint exige el rol " + grupo.getKey());
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("RS03: cada rol entra a su propio grupo")
    void elRolCorrectoEntra() throws Exception {
        for (Map.Entry<Rol, List<String>> grupo : GRUPOS.entrySet()) {
            for (String ruta : grupo.getValue()) {
                int estado = mvc.perform(get(ruta).session(sesiones.get(grupo.getKey())))
                        .andReturn().getResponse().getStatus();
                // 404 es valido: el id es inventado y la consulta ABAC no lo encuentra.
                assertThat(estado).as("%s en %s", grupo.getKey(), ruta).isIn(200, 404);
            }
        }
    }

    @Test
    @DisplayName("RS01: la politica no tiene dos reglas para el mismo metodo y patron")
    void laPoliticaNoTieneReglasDuplicadas() {
        Set<String> vistas = new HashSet<>();
        for (PoliticaAcceso.Regla regla : politica.reglas()) {
            assertThat(vistas.add(regla.descripcion()))
                    .as("regla duplicada: %s", regla.descripcion())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("RS05: ningun handler recibe del cliente el rol ni la identidad del usuario actual")
    void ningunHandlerRecibeRolNiIdentidad() {
        Set<String> prohibidos = Set.of("rol", "role", "esadmin", "isadmin", "admin",
                "idusuarioactual", "usuarioactual", "yo");
        // Unica excepcion: el filtro por rol del listado del admin. Filtra a OTROS usuarios;
        // la identidad y el rol de quien pide salen igual de la sesion.
        Set<String> permitidos = Set.of("GET /api/admin/usuarios#rol");

        List<String> violaciones = new ArrayList<>();
        for (Endpoint endpoint : InspectorDeEndpoints.de(contexto)) {
            if (endpoint.handlerMethod() == null) {
                continue;
            }
            for (MethodParameter parametro : endpoint.handlerMethod().getMethodParameters()) {
                String nombre = nombreDeRequest(parametro);
                if (nombre == null) {
                    continue;
                }
                String clave = endpoint.descripcion() + "#" + nombre;
                if (prohibidos.contains(nombre.toLowerCase()) && !permitidos.contains(clave)) {
                    violaciones.add(clave);
                }
            }
        }
        assertThat(violaciones).isEmpty();
    }

    /** Nombre con el que el parametro se lee del request, o null si no sale del request. */
    private static String nombreDeRequest(MethodParameter parametro) {
        parametro.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
        String declarado = null;
        if (parametro.hasParameterAnnotation(PathVariable.class)) {
            declarado = parametro.getParameterAnnotation(PathVariable.class).name();
        } else if (parametro.hasParameterAnnotation(RequestParam.class)) {
            declarado = parametro.getParameterAnnotation(RequestParam.class).name();
        } else if (parametro.hasParameterAnnotation(RequestHeader.class)) {
            declarado = parametro.getParameterAnnotation(RequestHeader.class).name();
        } else if (parametro.hasParameterAnnotation(CookieValue.class)) {
            declarado = parametro.getParameterAnnotation(CookieValue.class).name();
        } else {
            return null;
        }
        return declarado.isEmpty() ? parametro.getParameterName() : declarado;
    }
}
