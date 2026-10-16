package com.lossherpa.web;

import com.lossherpa.domain.EstadoSolicitud;
import com.lossherpa.domain.Usuario;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RS15: solicitudes de vinculacion, vinculos, aislamiento entre usuarios y RS10. */
class VinculacionTest extends TestIntegracion {

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

    // ------------------------------------------------------- separacion de roles

    @Test
    @DisplayName("un atleta en un endpoint de entrenador recibe 403")
    void atletaEnEndpointDeEntrenador() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/entrenador/atletas").session(sesion))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/entrenador/solicitudes").session(sesion))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un entrenador en un endpoint de atleta recibe 403")
    void entrenadorEnEndpointDeAtleta() throws Exception {
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/atleta/rutinas").session(sesion))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/atleta/entrenadores").session(sesion))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un atleta no puede entrar a los endpoints de admin")
    void atletaEnEndpointDeAdmin() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/admin/usuarios").session(sesion))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------- envio y listado

    @Test
    @DisplayName("el atleta ve el listado de entrenadores sin datos privados")
    void listaEntrenadoresSinDatosPrivados() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        String respuesta = mvc.perform(get("/api/atleta/entrenadores").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].aniosServicio").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta)
                .doesNotContain("@test.com")
                .doesNotContain("fechaNacimiento")
                .doesNotContain("passwordHash");
    }

    @Test
    @DisplayName("el listado de entrenadores filtra por nombre o apellido")
    void filtraEntrenadoresPorNombre() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/atleta/entrenadores").param("nombre", "paz").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].apellido").value("Paz"));
    }

    @Test
    @DisplayName("el atleta puede tener varias solicitudes pendientes a la vez")
    void variasSolicitudesPendientes() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        enviarSolicitud(sesion, entrenador.getId()).andExpect(status().isCreated());
        enviarSolicitud(sesion, otroEntrenador.getId()).andExpect(status().isCreated());

        mvc.perform(get("/api/atleta/solicitudes").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("no se puede mandar dos solicitudes al mismo entrenador")
    void noSePuedeDuplicarLaSolicitud() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        enviarSolicitud(sesion, entrenador.getId()).andExpect(status().isCreated());
        enviarSolicitud(sesion, entrenador.getId()).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("un atleta con entrenador activo no puede enviar solicitudes")
    void conEntrenadorActivoNoPuedeSolicitar() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        enviarSolicitud(sesion, otroEntrenador.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("Ya tenes un entrenador activo")));

        assertThat(solicitudes.count()).isZero();
    }

    @Test
    @DisplayName("una solicitud a un id inexistente devuelve 404")
    void solicitudAIdInexistente() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        enviarSolicitud(sesion, UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("no se puede mandar una solicitud al id de otro atleta")
    void solicitudAUnAtletaNoVale() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        enviarSolicitud(sesion, otroAtleta.getId()).andExpect(status().isNotFound());
        assertThat(solicitudes.count()).isZero();
    }

    @Test
    @DisplayName("mandar idAtleta en el body no permite solicitar en nombre de otro")
    void noSePuedeSolicitarEnNombreDeOtro() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(post("/api/atleta/solicitudes").with(csrf()).session(sesion)
                        .contentType(APPLICATION_JSON)
                        .content(cuerpo(Map.of(
                                "idEntrenador", entrenador.getId().toString(),
                                "idAtleta", otroAtleta.getId().toString()))))
                .andExpect(status().isBadRequest());

        assertThat(solicitudes.count()).isZero();
    }

    // --------------------------------------------------------------- cancelacion

    @Test
    @DisplayName("el atleta cancela su propia solicitud pendiente")
    void cancelaSuSolicitud() throws Exception {
        var solicitud = crearSolicitudPendiente(atleta, entrenador);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(post("/api/atleta/solicitudes/" + solicitud.getId() + "/cancelar")
                        .with(csrf()).session(sesion))
                .andExpect(status().isNoContent());

        assertThat(solicitudes.findById(solicitud.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.CANCELADA);
    }

    @Test
    @DisplayName("IDOR: un atleta no puede cancelar la solicitud de otro atleta")
    void noPuedeCancelarSolicitudAjena() throws Exception {
        var ajena = crearSolicitudPendiente(otroAtleta, entrenador);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(post("/api/atleta/solicitudes/" + ajena.getId() + "/cancelar")
                        .with(csrf()).session(sesion))
                .andExpect(status().isNotFound());

        assertThat(solicitudes.findById(ajena.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    // ----------------------------------------------------- aceptar y rechazar

    @Test
    @DisplayName("al aceptar, el vinculo queda activo y las demas pendientes se cancelan")
    void aceptarCancelaLasDemasPendientes() throws Exception {
        var elegida = crearSolicitudPendiente(atleta, entrenador);
        var otra = crearSolicitudPendiente(atleta, otroEntrenador);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(post("/api/entrenador/solicitudes/" + elegida.getId() + "/aceptar")
                        .with(csrf()).session(sesion))
                .andExpect(status().isNoContent());

        assertThat(solicitudes.findById(elegida.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(solicitudes.findById(otra.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.CANCELADA);
        assertThat(vinculos.buscarActivoPorAtleta(atleta.getId())).isPresent();
    }

    @Test
    @DisplayName("IDOR: un entrenador no puede aceptar una solicitud dirigida a otro")
    void noPuedeAceptarSolicitudAjena() throws Exception {
        var ajena = crearSolicitudPendiente(atleta, otroEntrenador);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(post("/api/entrenador/solicitudes/" + ajena.getId() + "/aceptar")
                        .with(csrf()).session(sesion))
                .andExpect(status().isNotFound());

        assertThat(solicitudes.findById(ajena.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(vinculos.count()).isZero();
    }

    @Test
    @DisplayName("el entrenador rechaza una solicitud y no se crea vinculo")
    void rechazarNoCreaVinculo() throws Exception {
        var solicitud = crearSolicitudPendiente(atleta, entrenador);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(post("/api/entrenador/solicitudes/" + solicitud.getId() + "/rechazar")
                        .with(csrf()).session(sesion))
                .andExpect(status().isNoContent());

        assertThat(solicitudes.findById(solicitud.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(vinculos.count()).isZero();
    }

    @Test
    @DisplayName("una solicitud ya resuelta no se puede volver a resolver")
    void noSePuedeResolverDosVeces() throws Exception {
        var solicitud = crearSolicitudPendiente(atleta, entrenador);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(post("/api/entrenador/solicitudes/" + solicitud.getId() + "/aceptar")
                        .with(csrf()).session(sesion))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/entrenador/solicitudes/" + solicitud.getId() + "/rechazar")
                        .with(csrf()).session(sesion))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("aceptar a un atleta que ya consiguio entrenador no crea un segundo vinculo")
    void noSePuedeTenerDosEntrenadores() throws Exception {
        var solicitud = crearSolicitudPendiente(atleta, entrenador);
        crearVinculoActivo(otroEntrenador, atleta);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(post("/api/entrenador/solicitudes/" + solicitud.getId() + "/aceptar")
                        .with(csrf()).session(sesion))
                .andExpect(status().isConflict());

        assertThat(vinculos.listarActivosPorEntrenador(entrenador.getId())).isEmpty();
    }

    // ------------------------------------------------------------- visibilidad

    @Test
    @DisplayName("el atleta ve el perfil publico de su entrenador, sin email")
    void atletaVeAsuEntrenador() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        String respuesta = mvc.perform(get("/api/atleta/entrenador").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entrenador.apellido").value("Lopez"))
                .andExpect(jsonPath("$.entrenador.edad").isNumber())
                .andExpect(jsonPath("$.desde").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta).doesNotContain("@test.com").doesNotContain("fechaNacimiento");
    }

    @Test
    @DisplayName("un atleta sin entrenador recibe 404 al pedir su entrenador")
    void atletaSinEntrenador() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/atleta/entrenador").session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el entrenador ve a su atleta activo")
    void entrenadorVeASuAtleta() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId()).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.pesoKg").value(72.5));
    }

    @Test
    @DisplayName("IDOR: el entrenador no puede ver un atleta que no es suyo")
    void noVeAtletaAjeno() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        crearVinculoActivo(otroEntrenador, otroAtleta);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + otroAtleta.getId()).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el entrenador no puede ver un atleta sin vinculo con nadie")
    void noVeAtletaSinVinculo() throws Exception {
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId()).session(sesion))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------- desvinculacion (RS10)

    @Test
    @DisplayName("RS10: al desvincular el atleta, el entrenador pierde acceso en el request siguiente")
    void elAtletaDesvinculaYElEntrenadorPierdeAcceso() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        MockHttpSession sesionEntrenador = iniciarSesion("beto@test.com");
        MockHttpSession sesionAtleta = iniciarSesion("ana@test.com");

        // Antes de desvincular el entrenador ve a su atleta.
        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId()).session(sesionEntrenador))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/atleta/vinculo").with(csrf()).session(sesionAtleta))
                .andExpect(status().isNoContent());

        // Misma sesion del entrenador, request siguiente: ya no tiene acceso.
        mvc.perform(get("/api/entrenador/atletas/" + atleta.getId()).session(sesionEntrenador))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/entrenador/atletas").session(sesionEntrenador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("RS10: el entrenador tambien puede desvincular, con efecto inmediato")
    void elEntrenadorDesvincula() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        MockHttpSession sesionEntrenador = iniciarSesion("beto@test.com");
        MockHttpSession sesionAtleta = iniciarSesion("ana@test.com");

        mvc.perform(delete("/api/entrenador/atletas/" + atleta.getId() + "/vinculo")
                        .with(csrf()).session(sesionEntrenador))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/atleta/entrenador").session(sesionAtleta))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("IDOR: el entrenador no puede desvincular a un atleta que no es suyo")
    void noPuedeDesvincularAtletaAjeno() throws Exception {
        crearVinculoActivo(otroEntrenador, otroAtleta);
        MockHttpSession sesion = iniciarSesion("beto@test.com");

        mvc.perform(delete("/api/entrenador/atletas/" + otroAtleta.getId() + "/vinculo")
                        .with(csrf()).session(sesion))
                .andExpect(status().isNotFound());

        assertThat(vinculos.buscarActivoPorAtleta(otroAtleta.getId())).isPresent();
    }

    @Test
    @DisplayName("despues de desvincularse el atleta puede volver a enviar solicitudes")
    void despuesDeDesvincularsePuedeSolicitarDeNuevo() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(delete("/api/atleta/vinculo").with(csrf()).session(sesion))
                .andExpect(status().isNoContent());
        enviarSolicitud(sesion, otroEntrenador.getId()).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------- historial

    @Test
    @DisplayName("el historial de exatletas solo trae nombre, apellido y fechas")
    void elHistorialEsMinimo() throws Exception {
        crearVinculoActivo(entrenador, atleta);
        MockHttpSession sesionAtleta = iniciarSesion("ana@test.com");
        mvc.perform(delete("/api/atleta/vinculo").with(csrf()).session(sesionAtleta))
                .andExpect(status().isNoContent());

        MockHttpSession sesion = iniciarSesion("beto@test.com");
        String respuesta = mvc.perform(get("/api/entrenador/exatletas").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ana"))
                .andExpect(jsonPath("$[0].fechaFin").exists())
                .andReturn().getResponse().getContentAsString();

        // Sin id no hay con que intentar pedir otros recursos del exatleta.
        assertThat(respuesta)
                .doesNotContain("\"id\"")
                .doesNotContain(atleta.getId().toString())
                .doesNotContain("pesoKg")
                .doesNotContain("edad")
                .doesNotContain("@test.com");
    }

    @Test
    @DisplayName("el entrenador solo ve su propio historial")
    void elHistorialEsPropio() throws Exception {
        var vinculoAjeno = crearVinculoActivo(otroEntrenador, otroAtleta);
        vinculoAjeno.cerrar(java.time.LocalDate.now());
        vinculos.save(vinculoAjeno);

        MockHttpSession sesion = iniciarSesion("beto@test.com");
        mvc.perform(get("/api/entrenador/exatletas").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------- RF3: unicidad bajo concurrencia

    @Test
    @DisplayName("dos aceptaciones simultaneas para el mismo atleta dejan un solo vinculo activo")
    void aceptacionesSimultaneasDejanUnSoloVinculo() throws Exception {
        var deBeto = crearSolicitudPendiente(atleta, entrenador);
        var deDario = crearSolicitudPendiente(atleta, otroEntrenador);
        MockHttpSession sesionBeto = iniciarSesion("beto@test.com");
        MockHttpSession sesionDario = iniciarSesion("dario@test.com");

        List<Integer> estados = enParalelo(
                () -> aceptar(sesionBeto, deBeto.getId()),
                () -> aceptar(sesionDario, deDario.getId()));

        // Una acepta (204). La otra: 409 si vio el vinculo, 404 si su solicitud ya habia sido
        // cancelada por la primera... pero nunca dos exitos ni un 500.
        assertThat(estados).filteredOn(e -> e == 204).hasSize(1);
        assertThat(estados).filteredOn(e -> e != 204).allMatch(e -> e == 409);
        assertThat(vinculos.listarTodosDe(atleta.getId()))
                .filteredOn(v -> v.estaActivo()).hasSize(1);
    }

    @Test
    @DisplayName("dos solicitudes simultaneas iguales dejan una sola pendiente")
    void solicitudesSimultaneasIgualesDejanUnaPendiente() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        List<Integer> estados = enParalelo(
                () -> enviarSolicitud(sesion, entrenador.getId()).andReturn()
                        .getResponse().getStatus(),
                () -> enviarSolicitud(sesion, entrenador.getId()).andReturn()
                        .getResponse().getStatus());

        assertThat(estados).containsExactlyInAnyOrder(201, 409);
        assertThat(solicitudes.listarPorAtletaYEstado(atleta.getId(), EstadoSolicitud.PENDIENTE))
                .hasSize(1);
    }

    @Test
    @DisplayName("la base rechaza un segundo vinculo activo del mismo atleta")
    void laBaseRechazaSegundoVinculoActivo() {
        crearVinculoActivo(entrenador, atleta);

        assertThatThrownBy(() -> crearVinculoActivo(otroEntrenador, atleta))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SQLITE_CONSTRAINT_UNIQUE");
    }

    @Test
    @DisplayName("la base permite vinculos cerrados del mismo atleta (historial)")
    void laBasePermiteHistorial() {
        var anterior = crearVinculoActivo(entrenador, atleta);
        anterior.cerrar(java.time.LocalDate.now());
        vinculos.saveAndFlush(anterior);

        crearVinculoActivo(otroEntrenador, atleta);

        assertThat(vinculos.listarTodosDe(atleta.getId())).hasSize(2);
    }

    @Test
    @DisplayName("la base rechaza una segunda pendiente del mismo par, pero no si la anterior se resolvio")
    void laBaseRechazaPendienteDuplicada() {
        var primera = crearSolicitudPendiente(atleta, entrenador);

        assertThatThrownBy(() -> crearSolicitudPendiente(atleta, entrenador))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SQLITE_CONSTRAINT_UNIQUE");

        primera.resolver(EstadoSolicitud.RECHAZADA);
        solicitudes.saveAndFlush(primera);
        crearSolicitudPendiente(atleta, entrenador);
    }

    @Test
    @DisplayName("al aceptar, las demas pendientes del atleta quedan CANCELADAS y la otra puede ya no aceptar")
    void laOtraSolicitudCanceladaNoSePuedeAceptar() throws Exception {
        var deBeto = crearSolicitudPendiente(atleta, entrenador);
        var deDario = crearSolicitudPendiente(atleta, otroEntrenador);

        aceptar(iniciarSesion("beto@test.com"), deBeto.getId());

        mvc.perform(post("/api/entrenador/solicitudes/" + deDario.getId() + "/aceptar")
                        .with(csrf()).session(iniciarSesion("dario@test.com")))
                .andExpect(status().isConflict());
        assertThat(vinculos.listarActivosPorEntrenador(otroEntrenador.getId())).isEmpty();
    }

    // -------------------------------------------------------- RF3: flujo completo

    @Test
    @DisplayName("flujo completo: buscar, solicitar, aceptar, ver, desvincular y aparecer como exatleta")
    void flujoCompleto() throws Exception {
        MockHttpSession sesionAtleta = iniciarSesion("ana@test.com");
        MockHttpSession sesionEntrenador = iniciarSesion("beto@test.com");

        mvc.perform(get("/api/atleta/entrenadores").param("nombre", "lopez").session(sesionAtleta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        enviarSolicitud(sesionAtleta, entrenador.getId()).andExpect(status().isCreated());

        mvc.perform(get("/api/entrenador/solicitudes").session(sesionEntrenador))
                .andExpect(jsonPath("$.length()").value(1));
        var pendiente = solicitudes.listarPorAtletaYEstado(
                atleta.getId(), EstadoSolicitud.PENDIENTE).get(0);
        assertThat(aceptar(sesionEntrenador, pendiente.getId())).isEqualTo(204);

        mvc.perform(get("/api/atleta/entrenador").session(sesionAtleta))
                .andExpect(status().isOk());
        mvc.perform(get("/api/entrenador/atletas").session(sesionEntrenador))
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(delete("/api/entrenador/atletas/" + atleta.getId() + "/vinculo")
                        .with(csrf()).session(sesionEntrenador))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/entrenador/atletas").session(sesionEntrenador))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/entrenador/exatletas").session(sesionEntrenador))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/atleta/entrenador").session(sesionAtleta))
                .andExpect(status().isNotFound());
    }

    private int aceptar(MockHttpSession sesion, UUID idSolicitud) throws Exception {
        return mvc.perform(post("/api/entrenador/solicitudes/" + idSolicitud + "/aceptar")
                        .with(csrf()).session(sesion))
                .andReturn().getResponse().getStatus();
    }

    /** Lanza las dos tareas a la vez (mismo latch de arranque) y devuelve sus estados HTTP. */
    private List<Integer> enParalelo(Callable<Integer> a, Callable<Integer> b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (Callable<Integer> tarea : List.of(a, b)) {
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    largada.await();
                    return tarea.call();
                }));
            }
            listos.await();
            largada.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> f : futuros) {
                estados.add(f.get(30, TimeUnit.SECONDS));
            }
            return estados;
        } finally {
            pool.shutdownNow();
        }
    }

    private org.springframework.test.web.servlet.ResultActions enviarSolicitud(
            MockHttpSession sesion, UUID idEntrenador) throws Exception {
        return mvc.perform(post("/api/atleta/solicitudes").with(csrf()).session(sesion)
                .contentType(APPLICATION_JSON)
                .content(cuerpo(Map.of("idEntrenador", idEntrenador.toString()))));
    }
}
