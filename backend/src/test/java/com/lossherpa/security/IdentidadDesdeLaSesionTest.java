package com.lossherpa.security;

import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.support.InspectorDeEndpoints;
import com.lossherpa.support.InspectorDeEndpoints.Endpoint;
import com.lossherpa.support.TestIntegracion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RS16 (flujo F4 del DFD) - La identidad que usan los servicios sale de la sesion.
 *
 * Inspecciona por reflexion todos los handlers reales:
 *
 *  - ningun dato que llega del cliente (path, query, body, cabecera, cookie) representa al
 *    usuario actual: ni su id, ni su rol, ni un flag del tipo esAdmin;
 *  - ningun handler lee cabeceras ni cookies: la sesion la resuelve Spring Security;
 *  - ningun controlador accede a un repositorio: todo pasa por un servicio, que toma el id
 *    de UsuarioActual.
 *
 * Se eligio reflexion y no ArchUnit: la suite corre offline (./mvnw -o) y ArchUnit no esta
 * en el repositorio local; ademas estas reglas necesitan el patron de cada endpoint, que
 * solo conoce el contexto de Spring.
 */
class IdentidadDesdeLaSesionTest extends TestIntegracion {

    /** Nombres que en cualquier endpoint representarian al usuario que hace la request. */
    private static final Set<String> IDENTIDAD = Set.of(
            "rol", "role", "esadmin", "isadmin", "admin", "yo", "me",
            "usuario", "usuarioid", "idusuario", "usuarioactual", "idusuarioactual", "userid");

    /** En la zona de un rol, el id de ese mismo rol tambien seria "yo". */
    private static final Map<String, Set<String>> IDENTIDAD_POR_ZONA = Map.of(
            "/api/atleta/", Set.of("atleta", "idatleta", "atletaid"),
            "/api/entrenador/", Set.of("entrenador", "identrenador", "entrenadorid"));

    /**
     * Excepciones justificadas. Clave: "METODO patron#campo".
     *  - El admin gestiona OTROS usuarios: idUsuario es el usuario gestionado y rol es un
     *    filtro o el rol de la cuenta que crea (RolRegistro no incluye ADMIN). La identidad
     *    del admin sale igual de la sesion.
     *  - En el registro publico, rol es el de la cuenta nueva, no el de quien pide.
     */
    private static final Set<String> PERMITIDOS = Set.of(
            "GET /api/admin/usuarios#rol",
            "POST /api/admin/usuarios#rol",
            "GET /api/admin/usuarios/{idUsuario}#idUsuario",
            "PUT /api/admin/usuarios/{idUsuario}#idUsuario",
            "DELETE /api/admin/usuarios/{idUsuario}#idUsuario",
            "POST /api/auth/registro#rol");

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("RS16: ningun parametro ni campo de body de un handler representa al usuario actual")
    void ningunDatoDelClienteEsLaIdentidad() {
        assertThat(datosDeIdentidad(InspectorDeEndpoints.de(contexto))).isEmpty();
    }

    @Test
    @DisplayName("RS16: ningun handler lee cabeceras ni cookies")
    void ningunHandlerLeeCabecerasNiCookies() {
        assertThat(lecturasDeCabecerasOCookies(InspectorDeEndpoints.de(contexto))).isEmpty();
    }

    @Test
    @DisplayName("RS16: ningun controlador accede a un repositorio, todo pasa por servicios")
    void losControladoresNoAccedenARepositorios() {
        assertThat(controladoresConRepositorio(InspectorDeEndpoints.de(contexto))).isEmpty();
    }

    @Test
    @DisplayName("RS16 (demostracion): un controlador que recibe la identidad del cliente se detecta")
    void unControladorQueRecibeLaIdentidadSeDetecta() {
        List<Endpoint> endpoints = InspectorDeEndpoints.deControladores(
                ControladorQueConfiaEnElCliente.class);

        assertThat(datosDeIdentidad(endpoints)).containsExactlyInAnyOrder(
                "GET /api/atleta/historial#idAtleta",
                "POST /api/perfil/rol#rol",
                "POST /api/perfil/rol#esAdmin");
        assertThat(lecturasDeCabecerasOCookies(endpoints))
                .containsExactly("GET /api/atleta/historial#X-Usuario");
        assertThat(controladoresConRepositorio(endpoints))
                .singleElement().asString().contains("ControladorQueConfiaEnElCliente");
    }

    // --- Verificacion ---

    static List<String> datosDeIdentidad(List<Endpoint> endpoints) {
        List<String> violaciones = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            if (endpoint.handlerMethod() == null) {
                continue;
            }
            for (MethodParameter parametro : endpoint.handlerMethod().getMethodParameters()) {
                for (String campo : camposDelCliente(parametro)) {
                    String clave = endpoint.descripcion() + "#" + campo;
                    if (esIdentidad(endpoint.patron(), campo) && !PERMITIDOS.contains(clave)) {
                        violaciones.add(clave);
                    }
                }
            }
        }
        return violaciones;
    }

    static List<String> lecturasDeCabecerasOCookies(List<Endpoint> endpoints) {
        List<String> lecturas = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            if (endpoint.handlerMethod() == null) {
                continue;
            }
            for (MethodParameter parametro : endpoint.handlerMethod().getMethodParameters()) {
                if (parametro.hasParameterAnnotation(RequestHeader.class)
                        || parametro.hasParameterAnnotation(CookieValue.class)) {
                    lecturas.add(endpoint.descripcion() + "#" + nombre(parametro));
                }
            }
        }
        return lecturas;
    }

    static List<String> controladoresConRepositorio(List<Endpoint> endpoints) {
        List<String> violaciones = new ArrayList<>();
        endpoints.stream()
                .filter(e -> e.handlerMethod() != null)
                .map(e -> e.handlerMethod().getBeanType())
                .filter(tipo -> tipo.getName().startsWith("com.lossherpa"))
                .distinct()
                .forEach(tipo -> {
                    for (Field campo : tipo.getDeclaredFields()) {
                        if (esAccesoADatos(campo.getType())) {
                            violaciones.add(tipo.getSimpleName() + "." + campo.getName());
                        }
                    }
                    for (Constructor<?> constructor : tipo.getDeclaredConstructors()) {
                        for (Class<?> parametro : constructor.getParameterTypes()) {
                            if (esAccesoADatos(parametro)) {
                                violaciones.add(tipo.getSimpleName() + "(" +
                                        parametro.getSimpleName() + ")");
                            }
                        }
                    }
                });
        return violaciones.stream().distinct().toList();
    }

    private static boolean esAccesoADatos(Class<?> tipo) {
        return Repository.class.isAssignableFrom(tipo) || EntityManager.class.isAssignableFrom(tipo);
    }

    private static boolean esIdentidad(String patron, String campo) {
        String normalizado = campo.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        if (IDENTIDAD.contains(normalizado)) {
            return true;
        }
        return IDENTIDAD_POR_ZONA.entrySet().stream()
                .anyMatch(zona -> patron.startsWith(zona.getKey())
                        && zona.getValue().contains(normalizado));
    }

    /**
     * Los nombres con los que el cliente puede mandar datos a traves de este parametro: el
     * nombre del parametro de path, query, cabecera o cookie, o los campos del body (y de
     * los records anidados) si es un @RequestBody o un @ModelAttribute.
     */
    private static List<String> camposDelCliente(MethodParameter parametro) {
        if (parametro.hasParameterAnnotation(PathVariable.class)
                || parametro.hasParameterAnnotation(RequestParam.class)
                || parametro.hasParameterAnnotation(RequestHeader.class)
                || parametro.hasParameterAnnotation(CookieValue.class)
                || parametro.hasParameterAnnotation(RequestAttribute.class)) {
            return List.of(nombre(parametro));
        }
        if (parametro.hasParameterAnnotation(RequestBody.class)
                || parametro.hasParameterAnnotation(ModelAttribute.class)) {
            List<String> campos = new ArrayList<>();
            camposDeRecord(ResolvableType.forMethodParameter(parametro), campos, 0);
            return campos;
        }
        return List.of();
    }

    private static void camposDeRecord(ResolvableType tipo, List<String> campos, int nivel) {
        Class<?> clase = tipo.resolve();
        if (clase == null || nivel > 3) {
            return;
        }
        if (Collection.class.isAssignableFrom(clase)) {
            camposDeRecord(tipo.asCollection().getGeneric(0), campos, nivel + 1);
            return;
        }
        if (!clase.isRecord()) {
            return;
        }
        for (RecordComponent componente : clase.getRecordComponents()) {
            campos.add(componente.getName());
            camposDeRecord(ResolvableType.forType(componente.getGenericType()), campos,
                    nivel + 1);
        }
    }

    private static String nombre(MethodParameter parametro) {
        parametro.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
        String declarado = "";
        if (parametro.hasParameterAnnotation(PathVariable.class)) {
            declarado = parametro.getParameterAnnotation(PathVariable.class).name();
        } else if (parametro.hasParameterAnnotation(RequestParam.class)) {
            declarado = parametro.getParameterAnnotation(RequestParam.class).name();
        } else if (parametro.hasParameterAnnotation(RequestHeader.class)) {
            declarado = parametro.getParameterAnnotation(RequestHeader.class).name();
        } else if (parametro.hasParameterAnnotation(CookieValue.class)) {
            declarado = parametro.getParameterAnnotation(CookieValue.class).name();
        } else if (parametro.hasParameterAnnotation(RequestAttribute.class)) {
            declarado = parametro.getParameterAnnotation(RequestAttribute.class).name();
        }
        return declarado.isEmpty() ? parametro.getParameterName() : declarado;
    }

    // --- Controlador de demostracion: todo lo que el test tiene que detectar ---

    record CambioDeRol(String rol, boolean esAdmin) {
    }

    /** Nunca se registra en la app: solo existe para probar que la verificacion falla. */
    @RestController
    static class ControladorQueConfiaEnElCliente {

        /** Acceso directo a datos desde el controlador, salteando el servicio. */
        private UsuarioRepository usuarios;

        @GetMapping("/api/atleta/historial")
        String historial(@RequestParam UUID idAtleta,
                         @RequestHeader("X-Usuario") String usuarioHeader) {
            return usuarios.findById(idAtleta).toString();
        }

        @PostMapping("/api/perfil/rol")
        String cambiarRol(@RequestBody CambioDeRol cambio) {
            return cambio.rol();
        }
    }
}
