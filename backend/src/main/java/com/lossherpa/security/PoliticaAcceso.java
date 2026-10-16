package com.lossherpa.security;

import com.lossherpa.domain.Rol;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Politica de autorizacion del backend: el punto de decision (PDP) unico.
 *
 * RS01 - Control de acceso centralizado. Cada endpoint esta declarado aca con su metodo, su
 *        patron exacto y el requisito de acceso. Los dos puntos de aplicacion (PEP) leen esta
 *        tabla: el filtro de seguridad por URL (SecurityConfig) y, de nuevo, el interceptor
 *        que corre sobre el handler que eligio MVC (InterceptorPoliticaAcceso). Ningun
 *        controlador decide por su cuenta que rol hace falta.
 * RS03 - Modelo hibrido RBAC + ABAC. El rol se decide aca. El atributo de relacion de cada
 *        endpoint (Atributo) queda declarado al lado, pero se aplica dentro de la consulta
 *        del repositorio: no existe "buscar y despues chequear".
 * RS05 - Controles solo en el servidor. La decision se toma con la Authentication que
 *        reconstruye FiltroSesionVigente desde la base, nunca con algo que mande el cliente.
 * RS08 - Control de acceso por rol en el servidor (definicion del Informe v1, a confirmar
 *        con el equipo: ver SECURITY.md).
 *
 * La tabla es la misma que figura en SECURITY.md ("Politica de autorizacion").
 */
@Component
public class PoliticaAcceso {

    /** RS03 (RBAC): quien puede invocar el endpoint. */
    public enum Acceso {
        /** Lista blanca publica: no exige sesion. */
        PUBLICO,
        /** Cualquier usuario con sesion valida, sea del rol que sea. */
        AUTENTICADO,
        ATLETA,
        ENTRENADOR,
        ADMIN,
        /** Ruta que existe en el contenedor pero nunca se expone (por ejemplo /error). */
        DENEGADO
    }

    /**
     * RS03 (ABAC): predicado de relacion que aplica la consulta del endpoint. Es declarativo:
     * documenta el criterio en un solo lugar, pero el where vive en el repositorio. Si el
     * predicado no se cumple la respuesta es 404 (RS04), no 403.
     */
    public enum Atributo {
        /** Sin recurso de otro usuario en juego. */
        NINGUNO,
        /** El recurso es el propio usuario de la sesion (UsuarioActual.id()). */
        PROPIO,
        /**
         * Recurso del usuario de la sesion: atleta.id = :yo o entrenador.id = :yo
         * (RutinaRepository.buscarDeAtleta, EjecucionRepository.listarPorAtleta,
         * SolicitudVinculacionRepository.buscarDeAtleta,
         * VinculoRepository.listarFinalizadosPorEntrenador).
         */
        DUENO,
        /**
         * Solicitud dirigida al entrenador de la sesion
         * (SolicitudVinculacionRepository.buscarDeEntrenador).
         */
        DESTINATARIO,
        /**
         * Vinculo vigente (fechaFin is null) entre el entrenador y el atleta
         * (VinculoRepository.buscarActivoPor*, ServicioVinculacion.vinculoActivoCon).
         */
        VINCULO_VIGENTE,
        /**
         * Vinculo vigente y ademas rutina ASIGNADA por el entrenador de la sesion
         * (RutinaRepository.listarAsignadasPorEntrenador,
         * EjecucionRepository.listarAsignadasPorEntrenador).
         */
        ASIGNADA_POR_MI,
        /**
         * Usuario que no es ADMIN
         * (UsuarioRepository.buscarGestionablePorAdmin, buscarParaAdmin).
         */
        GESTIONABLE
    }

    /**
     * Una regla: metodo + patron exacto + requisito. metodo null significa "cualquier
     * metodo" y solo se usa para rutas DENEGADO.
     */
    public record Regla(HttpMethod metodo, String patron, Acceso acceso, Atributo atributo) {

        public Regla {
            Objects.requireNonNull(patron);
            Objects.requireNonNull(acceso);
            Objects.requireNonNull(atributo);
            if (metodo == null && acceso != Acceso.DENEGADO) {
                throw new IllegalArgumentException(
                        "Solo una regla DENEGADO puede aplicar a cualquier metodo: " + patron);
            }
        }

        /** "GET /api/perfil": asi se nombra la regla en los mensajes y en los tests. */
        public String descripcion() {
            return (metodo == null ? "*" : metodo.name()) + " " + patron;
        }
    }

    private static final HttpMethod GET = HttpMethod.GET;
    private static final HttpMethod POST = HttpMethod.POST;
    private static final HttpMethod PUT = HttpMethod.PUT;
    private static final HttpMethod DELETE = HttpMethod.DELETE;

    /**
     * RS01: la tabla completa. Una regla por endpoint; lo que no esta aca se deniega.
     * El orden no importa porque ningun patron se superpone con otro.
     */
    static final List<Regla> REGLAS = List.of(
            // --- Lista blanca publica ---
            new Regla(GET, "/api/csrf", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(POST, "/api/auth/login", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(POST, "/api/auth/registro", Acceso.PUBLICO, Atributo.NINGUNO),

            // --- Cualquier rol, siempre sobre si mismo ---
            new Regla(GET, "/api/auth/yo", Acceso.AUTENTICADO, Atributo.PROPIO),
            new Regla(GET, "/api/perfil", Acceso.AUTENTICADO, Atributo.PROPIO),
            new Regla(PUT, "/api/perfil", Acceso.AUTENTICADO, Atributo.PROPIO),

            // --- Atleta ---
            new Regla(GET, "/api/atleta/rutinas", Acceso.ATLETA, Atributo.DUENO),
            new Regla(GET, "/api/atleta/rutinas/{idRutina}", Acceso.ATLETA, Atributo.DUENO),
            new Regla(POST, "/api/atleta/rutinas", Acceso.ATLETA, Atributo.DUENO),
            new Regla(POST, "/api/atleta/rutinas/{idRutina}/ejecuciones", Acceso.ATLETA,
                    Atributo.DUENO),
            new Regla(GET, "/api/atleta/ejecuciones", Acceso.ATLETA, Atributo.DUENO),
            new Regla(GET, "/api/atleta/entrenadores", Acceso.ATLETA, Atributo.NINGUNO),
            new Regla(GET, "/api/atleta/solicitudes", Acceso.ATLETA, Atributo.DUENO),
            new Regla(POST, "/api/atleta/solicitudes", Acceso.ATLETA, Atributo.DUENO),
            new Regla(POST, "/api/atleta/solicitudes/{idSolicitud}/cancelar", Acceso.ATLETA,
                    Atributo.DUENO),
            new Regla(GET, "/api/atleta/entrenador", Acceso.ATLETA, Atributo.VINCULO_VIGENTE),
            new Regla(DELETE, "/api/atleta/vinculo", Acceso.ATLETA, Atributo.VINCULO_VIGENTE),

            // --- Entrenador ---
            new Regla(GET, "/api/entrenador/atletas", Acceso.ENTRENADOR,
                    Atributo.VINCULO_VIGENTE),
            new Regla(GET, "/api/entrenador/exatletas", Acceso.ENTRENADOR, Atributo.DUENO),
            new Regla(GET, "/api/entrenador/solicitudes", Acceso.ENTRENADOR,
                    Atributo.DESTINATARIO),
            new Regla(POST, "/api/entrenador/solicitudes/{idSolicitud}/aceptar",
                    Acceso.ENTRENADOR, Atributo.DESTINATARIO),
            new Regla(POST, "/api/entrenador/solicitudes/{idSolicitud}/rechazar",
                    Acceso.ENTRENADOR, Atributo.DESTINATARIO),
            new Regla(GET, "/api/entrenador/atletas/{idAtleta}", Acceso.ENTRENADOR,
                    Atributo.VINCULO_VIGENTE),
            new Regla(DELETE, "/api/entrenador/atletas/{idAtleta}/vinculo", Acceso.ENTRENADOR,
                    Atributo.VINCULO_VIGENTE),
            new Regla(GET, "/api/entrenador/atletas/{idAtleta}/rutinas", Acceso.ENTRENADOR,
                    Atributo.ASIGNADA_POR_MI),
            new Regla(POST, "/api/entrenador/atletas/{idAtleta}/rutinas", Acceso.ENTRENADOR,
                    Atributo.VINCULO_VIGENTE),
            new Regla(GET, "/api/entrenador/atletas/{idAtleta}/ejecuciones", Acceso.ENTRENADOR,
                    Atributo.ASIGNADA_POR_MI),

            // --- Admin ---
            new Regla(GET, "/api/admin/usuarios", Acceso.ADMIN, Atributo.GESTIONABLE),
            new Regla(POST, "/api/admin/usuarios", Acceso.ADMIN, Atributo.NINGUNO),
            new Regla(GET, "/api/admin/usuarios/{idUsuario}", Acceso.ADMIN, Atributo.GESTIONABLE),
            new Regla(PUT, "/api/admin/usuarios/{idUsuario}", Acceso.ADMIN, Atributo.GESTIONABLE),
            new Regla(DELETE, "/api/admin/usuarios/{idUsuario}", Acceso.ADMIN,
                    Atributo.GESTIONABLE),

            // --- SPA y estaticos del build (RS13): solo GET ---
            new Regla(GET, "/", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/index.html", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/login", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/registro", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/entrenador", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/entrenador/**", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/atleta", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/atleta/**", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/admin", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/admin/**", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/assets/**", Acceso.PUBLICO, Atributo.NINGUNO),
            new Regla(GET, "/favicon.ico", Acceso.PUBLICO, Atributo.NINGUNO),

            // --- Rutas del contenedor que nunca se exponen ---
            // El BasicErrorController de Spring Boot existe como handler; ninguna request
            // (ni el forward de error del contenedor) lo puede usar para devolver datos.
            new Regla(null, "/error", Acceso.DENEGADO, Atributo.NINGUNO));

    private final List<Regla> reglas;

    public PoliticaAcceso() {
        this(REGLAS);
    }

    /** Solo para tests que necesitan una politica distinta de la declarada. */
    public PoliticaAcceso(List<Regla> reglas) {
        this.reglas = List.copyOf(reglas);
    }

    public List<Regla> reglas() {
        return reglas;
    }

    /** La regla exacta para un metodo y un patron, tal como los declara el handler. */
    public Optional<Regla> buscar(HttpMethod metodo, String patron) {
        return reglas.stream()
                .filter(regla -> regla.patron().equals(patron))
                .filter(regla -> regla.metodo() == null || regla.metodo().equals(metodo))
                .findFirst();
    }

    /**
     * RS01: la decision. No devuelve nada si se permite; si no, lanza:
     *  - AuthenticationCredentialsNotFoundException si no hay sesion y la regla la exige:
     *    termina en 401 (PuntoEntradaNoAutenticado);
     *  - AccesoDenegadoPorPolitica si hay sesion pero el rol no corresponde: termina en 403.
     *
     * La Authentication se pide de forma diferida: una regla PUBLICO o DENEGADO no la
     * necesita, y asi una ruta denegada sin sesion sigue siendo 403 (RS13).
     */
    public void exigir(Regla regla, Supplier<Authentication> autenticacion) {
        switch (regla.acceso()) {
            case PUBLICO -> {
            }
            case DENEGADO -> throw new AccesoDenegadoPorPolitica(
                    "ruta no expuesta: " + regla.descripcion());
            case AUTENTICADO -> exigirSesion(autenticacion.get());
            case ATLETA -> exigirRol(autenticacion.get(), Rol.ATLETA);
            case ENTRENADOR -> exigirRol(autenticacion.get(), Rol.ENTRENADOR);
            case ADMIN -> exigirRol(autenticacion.get(), Rol.ADMIN);
        }
    }

    private static void exigirSesion(Authentication autenticacion) {
        principal(autenticacion);
    }

    private static void exigirRol(Authentication autenticacion, Rol esperado) {
        // RS05: el rol sale del principal que FiltroSesionVigente recargo de la base en este
        // mismo request, no de nada que haya mandado el cliente.
        if (principal(autenticacion).getRol() != esperado) {
            throw new AccesoDenegadoPorPolitica("el endpoint exige el rol " + esperado);
        }
    }

    /**
     * Sin sesion, o con un principal que no es un usuario de la aplicacion (por ejemplo un
     * AnonymousAuthenticationToken): se trata como no autenticado y termina en 401.
     */
    private static UsuarioAutenticado principal(Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || !(autenticacion.getPrincipal() instanceof UsuarioAutenticado principal)) {
            throw new AuthenticationCredentialsNotFoundException("Sin usuario autenticado");
        }
        return principal;
    }
}
