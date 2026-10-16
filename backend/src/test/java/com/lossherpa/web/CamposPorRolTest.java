package com.lossherpa.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.lossherpa.domain.Ejecucion;
import com.lossherpa.domain.Rutina;
import com.lossherpa.domain.Usuario;
import com.lossherpa.domain.Vinculo;
import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RS06 - Control de acceso a nivel de campo (ASVS v5 8.2.3, BOPLA).
 *
 * Lectura: cada respuesta se compara contra el CONJUNTO EXACTO de claves que la matriz de
 * SECURITY.md permite para ese consumidor. No alcanza con que exista un campo: si alguien
 * agrega uno a un DTO por descuido, el test de ese endpoint falla.
 *
 * Escritura: cada campo que ningun consumidor puede escribir se manda a cada endpoint de
 * escritura; tiene que dar 400 y la base no puede cambiar.
 */
class CamposPorRolTest extends TestIntegracion {

    private Usuario ana;        // atleta de beto
    private Usuario beto;       // entrenador de ana
    private Usuario eva;        // atleta que le pidio vinculo a beto (todavia sin vinculo)
    private Usuario clara;      // exatleta de beto
    private Usuario dani;       // otro entrenador, sin relacion con ana
    private Usuario admin;
    private Rutina asignada;
    private Rutina propia;

    @BeforeEach
    void datos() {
        ana = conDescripcion(crearAtleta("ana@test.com", "Ana", "Suarez"), "Corro medias maratones");
        beto = crearEntrenador("beto@test.com", "Beto", "Lopez");
        eva = conDescripcion(crearAtleta("eva@test.com", "Eva", "Rios"), "Quiero ganar fuerza");
        clara = conDescripcion(crearAtleta("clara@test.com", "Clara", "Paz"), "Ex atleta");
        dani = crearEntrenador("dani@test.com", "Dani", "Mora");
        admin = crearAdmin("admin@test.com");

        crearVinculoActivo(beto, ana);
        Vinculo viejo = crearVinculoActivo(beto, clara);
        viejo.cerrar(LocalDate.now());
        vinculos.save(viejo);

        crearSolicitudPendiente(eva, beto);
        crearSolicitudPendiente(ana, dani);

        asignada = crearRutinaAsignada(beto, ana, "Fuerza 1");
        propia = crearRutinaPropia(ana, "Cardio propio");
        Ejecucion ejecucion = crearEjecucion(asignada, LocalDate.now().minusDays(1), 40.0);
        assertThat(ejecucion.getSeries()).isNotEmpty();
    }

    private Usuario conDescripcion(Usuario usuario, String descripcion) {
        usuario.setDescripcion(descripcion);
        return usuarios.save(usuario);
    }

    // ------------------------------------------------------------------ helpers de lectura

    private JsonNode leer(String url, String email) throws Exception {
        MockHttpSession sesion = iniciarSesion(email);
        String cuerpo = mvc.perform(get(url).session(sesion))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo);
    }

    private static Set<String> claves(JsonNode nodo) {
        Set<String> resultado = new TreeSet<>();
        Iterator<String> nombres = nodo.fieldNames();
        nombres.forEachRemaining(resultado::add);
        return resultado;
    }

    private static void exactamente(JsonNode nodo, String descripcion, String... esperadas) {
        assertThat(nodo.isObject()).as(descripcion + " es un objeto").isTrue();
        assertThat(claves(nodo)).as(descripcion).containsExactlyInAnyOrder(esperadas);
    }

    /** Un perfil de entrenador tal como lo ve un atleta. */
    private static final String[] ENTRENADOR_PARA_ATLETA =
            {"id", "nombre", "apellido", "edad", "descripcion", "aniosServicio"};

    /** Un perfil de atleta tal como lo ve su entrenador con vinculo vigente. */
    private static final String[] ATLETA_PARA_SU_ENTRENADOR =
            {"id", "nombre", "apellido", "edad", "descripcion", "pesoKg"};

    // -------------------------------------------------------------------- lectura: propio

    @Test
    @DisplayName("RS06 lectura: /api/auth/yo devuelve solo id, email, rol, nombre, apellido y tema")
    void sesionPropia() throws Exception {
        for (String email : List.of("ana@test.com", "beto@test.com", "admin@test.com")) {
            exactamente(leer("/api/auth/yo", email), "yo de " + email,
                    "id", "email", "rol", "nombre", "apellido", "tema");
        }
    }

    @Test
    @DisplayName("RS06 lectura: el perfil propio del atleta y del entrenador")
    void perfilPropio() throws Exception {
        exactamente(leer("/api/perfil", "ana@test.com"), "perfil de atleta",
                "id", "email", "rol", "nombre", "apellido", "fechaNacimiento", "edad",
                "descripcion", "pesoKg", "tema");
        exactamente(leer("/api/perfil", "beto@test.com"), "perfil de entrenador",
                "id", "email", "rol", "nombre", "apellido", "fechaNacimiento", "edad",
                "descripcion", "aniosServicio", "tema");
        exactamente(leer("/api/perfil", "admin@test.com"), "perfil de admin",
                "id", "email", "rol", "nombre", "apellido", "fechaNacimiento", "edad", "tema");
    }

    // ------------------------------------------------- lectura: lo que ve el atleta de otros

    @Test
    @DisplayName("RS06 lectura: el atleta ve de su entrenador solo el perfil publico, sin email ni fecha de nacimiento")
    void atletaVeASuEntrenador() throws Exception {
        JsonNode respuesta = leer("/api/atleta/entrenador", "ana@test.com");

        exactamente(respuesta, "mi entrenador", "entrenador", "desde");
        exactamente(respuesta.get("entrenador"), "perfil del entrenador",
                ENTRENADOR_PARA_ATLETA);
    }

    @Test
    @DisplayName("RS06 lectura: el listado de entrenadores y las solicitudes enviadas solo traen el perfil publico")
    void atletaVeEntrenadoresYSolicitudes() throws Exception {
        JsonNode entrenadores = leer("/api/atleta/entrenadores", "ana@test.com");
        assertThat(entrenadores.size()).isEqualTo(2);
        for (JsonNode entrenador : entrenadores) {
            exactamente(entrenador, "entrenador listado", ENTRENADOR_PARA_ATLETA);
        }

        JsonNode solicitudes = leer("/api/atleta/solicitudes", "ana@test.com");
        assertThat(solicitudes.size()).isEqualTo(1);
        exactamente(solicitudes.get(0), "solicitud enviada",
                "id", "estado", "creadaEn", "entrenador");
        exactamente(solicitudes.get(0).get("entrenador"), "entrenador de la solicitud",
                ENTRENADOR_PARA_ATLETA);
    }

    // --------------------------------------- lectura: lo que ve el entrenador de sus atletas

    @Test
    @DisplayName("RS06 lectura: el entrenador ve a su atleta vigente con edad y peso, nunca email ni fecha de nacimiento")
    void entrenadorVeASuAtleta() throws Exception {
        JsonNode lista = leer("/api/entrenador/atletas", "beto@test.com");
        assertThat(lista.size()).isEqualTo(1);
        exactamente(lista.get(0), "atleta activo", "atleta", "desde");
        exactamente(lista.get(0).get("atleta"), "perfil del atleta", ATLETA_PARA_SU_ENTRENADOR);

        exactamente(leer("/api/entrenador/atletas/" + ana.getId(), "beto@test.com"),
                "detalle del atleta", ATLETA_PARA_SU_ENTRENADOR);
    }

    @Test
    @DisplayName("RS06 lectura: antes del vinculo el entrenador ve del solicitante solo nombre, apellido y edad")
    void entrenadorSinVinculoVeLoMinimo() throws Exception {
        JsonNode solicitudes = leer("/api/entrenador/solicitudes", "beto@test.com");

        assertThat(solicitudes.size()).isEqualTo(1);
        exactamente(solicitudes.get(0), "solicitud recibida", "id", "estado", "creadaEn", "atleta");
        exactamente(solicitudes.get(0).get("atleta"), "solicitante antes del vinculo",
                "nombre", "apellido", "edad");
    }

    @Test
    @DisplayName("RS06 lectura: del exatleta el entrenador conserva solo nombre y las fechas del vinculo")
    void entrenadorVeAlExatleta() throws Exception {
        JsonNode lista = leer("/api/entrenador/exatletas", "beto@test.com");

        assertThat(lista.size()).isEqualTo(1);
        exactamente(lista.get(0), "exatleta", "nombre", "apellido", "fechaInicio", "fechaFin");
    }

    // ------------------------------------------------------- lectura: rutinas y ejecuciones

    private static final String[] RUTINA = {"id", "nombre", "origen", "creadaEn", "creadaPor", "ejercicios"};
    private static final String[] EJERCICIO = {"id", "orden", "nombre", "series", "repeticiones"};
    private static final String[] EJECUCION = {"id", "fecha", "idRutina", "nombreRutina", "series"};
    private static final String[] SERIE = {"idEjercicio", "nombreEjercicio", "nroSerie", "cargaKg"};

    private void verificarRutina(JsonNode rutina, boolean conCreador, String quien) {
        List<String> esperadas = new ArrayList<>(List.of(RUTINA));
        if (!conCreador) {
            esperadas.remove("creadaPor");
        }
        exactamente(rutina, "rutina vista por " + quien, esperadas.toArray(String[]::new));
        for (JsonNode ejercicio : rutina.get("ejercicios")) {
            exactamente(ejercicio, "ejercicio visto por " + quien, EJERCICIO);
        }
    }

    @Test
    @DisplayName("RS06 lectura: el atleta ve sus rutinas con las mismas claves que el entrenador que las asigno")
    void rutinas() throws Exception {
        JsonNode delAtleta = leer("/api/atleta/rutinas", "ana@test.com");
        assertThat(delAtleta.size()).isEqualTo(2);
        for (JsonNode rutina : delAtleta) {
            verificarRutina(rutina, "ASIGNADA".equals(rutina.get("origen").asText()), "el atleta");
        }

        JsonNode delEntrenador = leer("/api/entrenador/atletas/" + ana.getId() + "/rutinas",
                "beto@test.com");
        assertThat(delEntrenador.size()).as("el entrenador solo ve las que el asigno").isEqualTo(1);
        verificarRutina(delEntrenador.get(0), true, "el entrenador");
        // El entrenador ve el nombre de quien la creo, no su id ni su perfil.
        assertThat(delEntrenador.get(0).get("creadaPor").asText()).isEqualTo("Beto Lopez");
        assertThat(delEntrenador.toString()).doesNotContain(propia.getNombre());
    }

    @Test
    @DisplayName("RS06 lectura: el historial de ejecuciones tiene las mismas claves para el atleta y para su entrenador")
    void ejecuciones() throws Exception {
        List<JsonNode> historiales = List.of(
                leer("/api/atleta/ejecuciones", "ana@test.com"),
                leer("/api/entrenador/atletas/" + ana.getId() + "/ejecuciones", "beto@test.com"));

        for (JsonNode historial : historiales) {
            assertThat(historial.size()).isEqualTo(1);
            exactamente(historial.get(0), "ejecucion", EJECUCION);
            for (JsonNode serie : historial.get(0).get("series")) {
                exactamente(serie, "serie ejecutada", SERIE);
            }
        }
    }

    // --------------------------------------------------------------- lectura: el admin

    @Test
    @DisplayName("RS06 lectura: el admin ve la ficha completa de atletas y entrenadores, sin hash")
    void adminVeLaFichaCompleta() throws Exception {
        JsonNode lista = leer("/api/admin/usuarios", "admin@test.com");
        assertThat(lista.size()).isEqualTo(5);
        for (JsonNode usuario : lista) {
            boolean atleta = "ATLETA".equals(usuario.get("rol").asText());
            List<String> esperadas = new ArrayList<>(List.of("id", "email", "rol", "nombre",
                    "apellido", "fechaNacimiento", "edad", "creadoEn"));
            if (usuario.has("descripcion")) {
                esperadas.add("descripcion");
            }
            esperadas.add(atleta ? "pesoKg" : "aniosServicio");
            exactamente(usuario, "usuario visto por el admin", esperadas.toArray(String[]::new));
        }

        JsonNode detalle = leer("/api/admin/usuarios/" + beto.getId(), "admin@test.com");
        exactamente(detalle, "detalle de entrenador visto por el admin", "id", "email", "rol",
                "nombre", "apellido", "fechaNacimiento", "edad", "descripcion", "aniosServicio",
                "creadoEn");
    }

    // ------------------------------------------------- lectura: nada de lo privado se filtra

    @Test
    @DisplayName("RS06 lectura: ninguna respuesta a un tercero contiene el email, la fecha de nacimiento ni el hash")
    void ningunDatoPrivadoSeFiltra() throws Exception {
        String paraElEntrenador = leer("/api/entrenador/atletas", "beto@test.com").toString()
                + leer("/api/entrenador/atletas/" + ana.getId(), "beto@test.com")
                + leer("/api/entrenador/solicitudes", "beto@test.com")
                + leer("/api/entrenador/exatletas", "beto@test.com");
        String paraElAtleta = leer("/api/atleta/entrenador", "ana@test.com").toString()
                + leer("/api/atleta/entrenadores", "ana@test.com")
                + leer("/api/atleta/solicitudes", "ana@test.com");

        assertThat(paraElEntrenador)
                .doesNotContain("@test.com")
                .doesNotContain(ana.getFechaNacimiento().toString())
                .doesNotContain(eva.getFechaNacimiento().toString())
                .doesNotContain("passwordHash", "$2a$", "$2b$");
        assertThat(paraElAtleta)
                .doesNotContain("@test.com")
                .doesNotContain(beto.getFechaNacimiento().toString())
                .doesNotContain("passwordHash", "$2a$", "$2b$");
    }

    // ------------------------------------------------------------------ helpers de escritura

    private int enviar(HttpMethod metodo, String url, MockHttpSession sesion, Object cuerpo)
            throws Exception {
        MockHttpServletRequestBuilder peticion = request(metodo, url).with(csrf())
                .contentType(APPLICATION_JSON)
                .content(cuerpo(cuerpo));
        if (sesion != null) {
            peticion.session(sesion);
        }
        return mvc.perform(peticion).andReturn().getResponse().getStatus();
    }

    /** Foto de todo lo que una escritura podria cambiar. */
    private List<String> instantanea() {
        List<String> foto = new ArrayList<>();
        usuarios.findAll().forEach(u -> foto.add(String.join("|", "usuario",
                String.valueOf(u.getId()), u.getEmail(), String.valueOf(u.getRol()),
                u.getNombre(), u.getApellido(), String.valueOf(u.getFechaNacimiento()),
                String.valueOf(u.getDescripcion()), String.valueOf(u.getPesoKg()),
                String.valueOf(u.getAniosServicio()), u.getPasswordHash())));
        foto.add("rutinas=" + rutinas.count());
        foto.add("ejecuciones=" + ejecuciones.count());
        foto.add("solicitudes=" + solicitudes.count());
        foto.add("vinculos=" + vinculos.count());
        Collections.sort(foto);
        return foto;
    }

    /**
     * Manda el cuerpo base con cada campo prohibido agregado, de a uno: todos tienen que dar
     * 400 y la base no puede cambiar. Despues manda el cuerpo base solo, para probar que el
     * 400 era por el campo extra y no porque el cuerpo base estuviera mal armado.
     */
    private void rechazaCamposProhibidos(String endpoint, HttpMethod metodo, String url,
                                         MockHttpSession sesion, Map<String, Object> base,
                                         List<String> prohibidos) throws Exception {
        List<String> antes = instantanea();

        for (String campo : prohibidos) {
            Map<String, Object> cuerpo = new HashMap<>(base);
            cuerpo.put(campo, campo.equals("rol") ? "ADMIN" : "valor-inyectado");

            assertThat(enviar(metodo, url, sesion, cuerpo))
                    .as(endpoint + " con el campo '" + campo + "'")
                    .isEqualTo(400);
        }
        assertThat(instantanea()).as(endpoint + ": la base no cambia").isEqualTo(antes);

        assertThat(enviar(metodo, url, sesion, base))
                .as(endpoint + ": el cuerpo base solo es valido")
                .isIn(200, 201);
    }

    private static final List<String> NO_ESCRIBIBLES_DE_USUARIO =
            List.of("rol", "email", "id", "passwordHash", "creadoEn", "atleta", "origen",
                    "entrenadorCreador");

    private Map<String, Object> perfilAtleta() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Ana Maria");
        datos.put("apellido", "Suarez");
        datos.put("fechaNacimiento", "1998-03-12");
        datos.put("descripcion", "Corro medias maratones");
        datos.put("pesoKg", 70.0);
        return datos;
    }

    private Map<String, Object> perfilEntrenador() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Beto");
        datos.put("apellido", "Lopez");
        datos.put("fechaNacimiento", "1985-07-30");
        datos.put("aniosServicio", 10);
        return datos;
    }

    private Map<String, Object> altaAtleta() {
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

    private Map<String, Object> rutinaNueva() {
        return Map.of("nombre", "Rutina nueva", "ejercicios", List.of(
                Map.of("nombre", "Sentadilla", "series", 3, "repeticiones", 8)));
    }

    // ----------------------------------------------------------- escritura: usuario

    @Test
    @DisplayName("RS06 escritura: PUT /api/perfil rechaza cada campo que el usuario no puede escribir")
    void perfilPropioRechazaCamposProhibidos() throws Exception {
        rechazaCamposProhibidos("PUT /api/perfil (atleta)", HttpMethod.PUT, "/api/perfil",
                iniciarSesion("ana@test.com"), perfilAtleta(), NO_ESCRIBIBLES_DE_USUARIO);
        rechazaCamposProhibidos("PUT /api/perfil (entrenador)", HttpMethod.PUT, "/api/perfil",
                iniciarSesion("beto@test.com"), perfilEntrenador(), NO_ESCRIBIBLES_DE_USUARIO);
    }

    @Test
    @DisplayName("RS06 escritura: PUT /api/admin/usuarios/{id} rechaza los mismos campos, tambien para el admin")
    void adminRechazaCamposProhibidos() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");

        rechazaCamposProhibidos("PUT admin sobre un atleta", HttpMethod.PUT,
                "/api/admin/usuarios/" + ana.getId(), sesion, perfilAtleta(),
                NO_ESCRIBIBLES_DE_USUARIO);
        rechazaCamposProhibidos("PUT admin sobre un entrenador", HttpMethod.PUT,
                "/api/admin/usuarios/" + beto.getId(), sesion, perfilEntrenador(),
                NO_ESCRIBIBLES_DE_USUARIO);
    }

    @Test
    @DisplayName("RS06 escritura: el alta (registro publico y alta por el admin) rechaza campos que el cliente no decide")
    void altasRechazanCamposProhibidos() throws Exception {
        List<String> prohibidos = List.of("id", "passwordHash", "creadoEn", "atleta", "origen",
                "entrenadorCreador");

        rechazaCamposProhibidos("POST /api/auth/registro", HttpMethod.POST, "/api/auth/registro",
                null, altaAtleta(), prohibidos);

        Map<String, Object> otra = altaAtleta();
        otra.put("email", "otra.atleta@test.com");
        rechazaCamposProhibidos("POST /api/admin/usuarios", HttpMethod.POST,
                "/api/admin/usuarios", iniciarSesion("admin@test.com"), otra, prohibidos);
    }

    // ---------------------------------------------------- escritura: campos de otro rol

    @Test
    @DisplayName("RS06 escritura: el admin no puede ponerle peso a un entrenador ni anos de servicio a un atleta")
    void adminNoMezclaCamposDeRol() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");
        List<String> antes = instantanea();

        Map<String, Object> pesoAUnEntrenador = perfilEntrenador();
        pesoAUnEntrenador.remove("aniosServicio");
        pesoAUnEntrenador.put("pesoKg", 80.0);
        assertThat(enviar(HttpMethod.PUT, "/api/admin/usuarios/" + beto.getId(), sesion,
                pesoAUnEntrenador)).isEqualTo(400);

        Map<String, Object> ambosCampos = perfilEntrenador();
        ambosCampos.put("pesoKg", 80.0);
        assertThat(enviar(HttpMethod.PUT, "/api/admin/usuarios/" + beto.getId(), sesion,
                ambosCampos)).isEqualTo(400);

        Map<String, Object> aniosAUnAtleta = perfilAtleta();
        aniosAUnAtleta.put("aniosServicio", 5);
        assertThat(enviar(HttpMethod.PUT, "/api/admin/usuarios/" + ana.getId(), sesion,
                aniosAUnAtleta)).isEqualTo(400);

        Map<String, Object> atletaSinPeso = perfilAtleta();
        atletaSinPeso.remove("pesoKg");
        assertThat(enviar(HttpMethod.PUT, "/api/admin/usuarios/" + ana.getId(), sesion,
                atletaSinPeso)).as("el campo propio del rol tambien es obligatorio").isEqualTo(400);

        assertThat(instantanea()).isEqualTo(antes);
    }

    @Test
    @DisplayName("RS06 escritura: el alta por el admin tampoco mezcla campos de rol")
    void altaDelAdminNoMezclaCamposDeRol() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");
        List<String> antes = instantanea();

        Map<String, Object> entrenadorConPeso = altaAtleta();
        entrenadorConPeso.put("rol", "ENTRENADOR");
        entrenadorConPeso.put("aniosServicio", 3);
        assertThat(enviar(HttpMethod.POST, "/api/admin/usuarios", sesion, entrenadorConPeso))
                .isEqualTo(400);

        Map<String, Object> atletaConAnios = altaAtleta();
        atletaConAnios.put("aniosServicio", 3);
        assertThat(enviar(HttpMethod.POST, "/api/admin/usuarios", sesion, atletaConAnios))
                .isEqualTo(400);

        assertThat(instantanea()).isEqualTo(antes);
    }

    @Test
    @DisplayName("RS06 escritura: el admin editando su propio perfil tampoco puede ponerse peso ni anos de servicio")
    void adminNoSeAgregaCamposDeRol() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@test.com");
        List<String> antes = instantanea();

        Map<String, Object> conPeso = new HashMap<>();
        conPeso.put("nombre", "Admin");
        conPeso.put("apellido", "Sistema");
        conPeso.put("fechaNacimiento", "1990-01-01");
        conPeso.put("pesoKg", 80.0);
        assertThat(enviar(HttpMethod.PUT, "/api/perfil", sesion, conPeso)).isEqualTo(400);

        Map<String, Object> conAnios = new HashMap<>(conPeso);
        conAnios.remove("pesoKg");
        conAnios.put("aniosServicio", 5);
        assertThat(enviar(HttpMethod.PUT, "/api/perfil", sesion, conAnios)).isEqualTo(400);

        assertThat(instantanea()).isEqualTo(antes);
    }

    // ---------------------------------------------- escritura: rutinas, ejecuciones, solicitudes

    @Test
    @DisplayName("RS06 escritura: las rutinas (alta y edicion) rechazan origen, dueno, creador y fecha, y tambien dentro de cada ejercicio")
    void rutinasRechazanCamposProhibidos() throws Exception {
        List<String> prohibidos = List.of("id", "atleta", "idAtleta", "origen",
                "entrenadorCreador", "creadaEn", "creadoEn");

        rechazaCamposProhibidos("POST /api/atleta/rutinas", HttpMethod.POST,
                "/api/atleta/rutinas", iniciarSesion("ana@test.com"), rutinaNueva(), prohibidos);
        rechazaCamposProhibidos("POST /api/entrenador/atletas/{id}/rutinas", HttpMethod.POST,
                "/api/entrenador/atletas/" + ana.getId() + "/rutinas",
                iniciarSesion("beto@test.com"), rutinaNueva(), prohibidos);

        // RF2: la edicion es una lista blanca, no una lista negra; un campo extra cualquiera
        // (por ejemplo "id") tambien da 400 en lugar de ignorarse.
        Rutina editable = crearRutinaAsignada(beto, ana, "Para editar");
        rechazaCamposProhibidos("PUT /api/entrenador/atletas/{id}/rutinas/{idRutina}", HttpMethod.PUT,
                "/api/entrenador/atletas/" + ana.getId() + "/rutinas/" + editable.getId(),
                iniciarSesion("beto@test.com"), rutinaNueva(), prohibidos);

        // Campos que el cliente tampoco decide a nivel de ejercicio.
        List<String> antes = instantanea();
        for (String campo : List.of("id", "orden", "rutina")) {
            Map<String, Object> ejercicio = new HashMap<>(
                    Map.of("nombre", "Press", "series", 3, "repeticiones", 8));
            ejercicio.put(campo, "valor-inyectado");
            Map<String, Object> cuerpo = Map.of("nombre", "Con ejercicio trucho",
                    "ejercicios", List.of(ejercicio));

            assertThat(enviar(HttpMethod.POST, "/api/atleta/rutinas",
                    iniciarSesion("ana@test.com"), cuerpo))
                    .as("ejercicio con '" + campo + "'").isEqualTo(400);
        }
        assertThat(instantanea()).isEqualTo(antes);
    }

    @Test
    @DisplayName("RS06 escritura: la ejecucion rechaza atleta, rutina y fecha de creacion, tambien dentro de cada serie")
    void ejecucionesRechazanCamposProhibidos() throws Exception {
        MockHttpSession sesion = iniciarSesion("ana@test.com");
        String url = "/api/atleta/rutinas/" + asignada.getId() + "/ejecuciones";
        Map<String, Object> base = new HashMap<>(cuerpoEjecucion(asignada, LocalDate.now(), 45.0));

        rechazaCamposProhibidos("POST ejecuciones", HttpMethod.POST, url, sesion, base,
                List.of("id", "atleta", "rutina", "idRutina", "creadaEn"));

        List<String> antes = instantanea();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> series = (List<Map<String, Object>>) base.get("series");
        for (String campo : List.of("id", "ejecucion", "creadaEn")) {
            List<Map<String, Object>> conCampo = new ArrayList<>();
            for (Map<String, Object> serie : series) {
                Map<String, Object> copia = new HashMap<>(serie);
                copia.put(campo, "valor-inyectado");
                conCampo.add(copia);
            }
            Map<String, Object> cuerpo = new HashMap<>(base);
            cuerpo.put("series", conCampo);

            assertThat(enviar(HttpMethod.POST, url, sesion, cuerpo))
                    .as("serie con '" + campo + "'").isEqualTo(400);
        }
        assertThat(instantanea()).isEqualTo(antes);
    }

    @Test
    @DisplayName("RS06 escritura: la solicitud solo acepta el entrenador; estado, atleta e id los pone el servidor")
    void solicitudesRechazanCamposProhibidos() throws Exception {
        Usuario sinEntrenador = crearAtleta("lola@test.com", "Lola", "Gil");
        Map<String, Object> base = Map.of("idEntrenador", dani.getId().toString());

        rechazaCamposProhibidos("POST /api/atleta/solicitudes", HttpMethod.POST,
                "/api/atleta/solicitudes", iniciarSesion(sinEntrenador.getEmail()), base,
                List.of("id", "idAtleta", "atleta", "estado", "creadaEn", "resueltaEn"));
    }
}
