# Controles de seguridad — Los Sherpa

Mapeo de cada requerimiento de seguridad del sprint con dónde está implementado y qué tests
lo cubren. En el código, cada punto está marcado con un comentario (`// RS04`, `// RS10`, …).

Las rutas de clases son relativas a `backend/src/main/java/com/lossherpa/` y las de tests a
`backend/src/test/java/com/lossherpa/`.

**Estado de la suite: 173 tests, todos en verde** (`cd backend && ./mvnw test`).

---

## Resumen

| RS | Requisito | Estado |
|---|---|---|
| RS04 | Control de acceso a nivel de dato (ASVS 8.2.2) | ✅ |
| RS07 | No exposición de IDs | ✅ |
| RS10 | Impacto inmediato de cambios de autorización (ASVS 8.3.2) | ✅ |
| RS12 | Logs de autorización | ✅ |
| RS13 | Deny by default, incluidos los estáticos | ✅ |
| RS14 | Sin configuración por defecto | ✅ |
| RS15 | Tests unitarios y de integración | ✅ |
| RS17 | Protección de logs | ✅ |

---

## RS04 — Control de acceso a nivel de dato (ASVS v5 8.2.2)

**Cómo está implementado.** La regla estructural es que **el id del usuario autenticado nunca
viene del cliente**: se lee del `SecurityContext` y se pasa como parámetro a la consulta. No
existe en ningún repositorio una búsqueda "pelada" por id de un recurso de usuario, así que
el patrón "buscar por id y después chequear el dueño" no se puede escribir por accidente.

Para el entrenador, la autorización es parte de la propia consulta SQL: exige
`origen = ASIGNADA`, `entrenadorCreador = :yo` y un `exists` de vínculo con
`fechaFin is null`. Si no se cumple, la consulta no devuelve filas; no hay un `if` posterior
que alguien pueda olvidar.

| Dónde | Qué hace |
|---|---|
| `security/UsuarioActual.java` | Única fuente del id autenticado (`id()`, `rol()`) |
| `service/ServicioUsuarios.java` | `autenticado()`, `autenticadoConRol(...)` |
| `repository/RutinaRepository.java` | `buscarDeAtleta`, `listarPorAtleta`, `listarAsignadasPorEntrenador`, `buscarAsignadaDeEntrenador` |
| `repository/EjecucionRepository.java` | `listarPorAtleta`, `listarAsignadasPorEntrenador` |
| `repository/SolicitudVinculacionRepository.java` | `buscarDeAtleta`, `buscarDeEntrenador` |
| `repository/VinculoRepository.java` | `buscarActivoPorAtleta`, `buscarActivoPorEntrenadorYAtleta` |
| `service/ServicioVinculacion.java` | `vinculoActivoCon(...)`: punto único donde el entrenador prueba su relación vigente |
| `service/ServicioEjecuciones.java` | `registrar(...)`: los `idEjercicio` del cliente se validan contra los ejercicios de **esa** rutina |
| `repository/UsuarioRepository.java` | `buscarGestionablePorAdmin`: excluye a los ADMIN en el `where` |

**Decisión: la denegación a nivel de dato devuelve 404, no 403.** Un 403 confirmaría que el
recurso existe y permitiría enumerar UUIDs ajenos. El 403 queda reservado para "rol
equivocado", donde no hay nada que enumerar. El intento igual queda registrado en el log de
auditoría con el motivo real (ver RS12).

**Tests**

| Caso | Test |
|---|---|
| Atleta lee la rutina de otro atleta | `web/RutinasTest.noLeeRutinaAjena` |
| Atleta completa la rutina de otro atleta | `web/RutinasTest.noCompletaRutinaAjena` |
| Entrenador ve rutinas de un atleta ajeno | `web/RutinasTest.noVeRutinasDeAtletaAjeno` |
| Entrenador ve el historial de un atleta ajeno | `web/EjecucionesTest.noVeHistorialDeAtletaAjeno` |
| Entrenador ve el perfil de un atleta ajeno | `web/VinculacionTest.noVeAtletaAjeno`, `noVeAtletaSinVinculo` |
| Entrenador crea una rutina para un atleta ajeno | `web/RutinasTest.noCreaRutinaParaAtletaAjeno`, `noCreaRutinaSinVinculo` |
| Atleta cancela la solicitud de otro | `web/VinculacionTest.noPuedeCancelarSolicitudAjena` |
| Entrenador acepta una solicitud dirigida a otro | `web/VinculacionTest.noPuedeAceptarSolicitudAjena` |
| Entrenador desvincula a un atleta ajeno | `web/VinculacionTest.noPuedeDesvincularAtletaAjeno` |
| Historial de ejecuciones de otro usuario | `web/EjecucionesTest.elHistorialNoFiltraDeOtros` |
| Serie de un ejercicio de otra rutina | `web/EjecucionesTest.serieDeOtraRutinaSeRechaza` |
| Solicitud en nombre de otro atleta | `web/VinculacionTest.noSePuedeSolicitarEnNombreDeOtro` |
| Historial de exatletas de otro entrenador | `web/VinculacionTest.elHistorialEsPropio` |

---

## RS07 — No exposición de IDs

**Cómo está implementado.** El UUIDv4 es la **clave primaria real** de todas las tablas, no
una columna pública en paralelo a un id numérico. No existe ningún id secuencial que se pueda
filtrar. Se genera con `@UuidGenerator(style = RANDOM)` (versión 4) y se almacena como
`varchar(36)`.

Todas las respuestas usan DTOs de salida explícitos (records). **Nunca se serializa una
entidad JPA**, así que no hay forma de que el hash de la contraseña o una relación entren en
una respuesta por descuido.

| Dónde | Qué hace |
|---|---|
| `domain/*.java` (7 entidades) | Bloque `@Id` con `@UuidGenerator(style = RANDOM)` |
| `web/dto/out/SesionResponse.java` | id, email, rol, nombre, apellido |
| `web/dto/out/PerfilResponse.java` | perfil propio, con la edad calculada |
| `web/dto/out/PerfilPublicoResponse.java` | **una fábrica por rol** (`deEntrenador`, `deAtleta`): cada una llena solo los campos de su rol |
| `web/dto/out/ExatletaResponse.java` | **sin el id del atleta**: solo nombre, apellido y fechas |
| `web/dto/out/RutinaResponse.java` | del entrenador creador expone solo nombre y apellido, no su id |
| `web/dto/out/EjecucionResponse.java`, `SolicitudEnviadaResponse.java`, `SolicitudRecibidaResponse.java`, `AtletaActivoResponse.java`, `MiEntrenadorResponse.java`, `UsuarioAdminResponse.java` | resto de las salidas |

**Decisión.** El historial de exatletas no incluye el id del atleta. El enunciado permite
conservar nombre, apellido y fechas; sin el id, el entrenador ni siquiera tiene con qué
intentar pedir otro recurso de ese exatleta.

**Tests**

| Caso | Test |
|---|---|
| El registro no devuelve el hash | `web/RegistroTest.laRespuestaNoFiltraElHash` |
| El perfil no devuelve el hash | `web/PerfilTest.devuelveElPerfilPropio` |
| El listado de entrenadores no filtra email ni fecha de nacimiento | `web/VinculacionTest.listaEntrenadoresSinDatosPrivados` |
| El perfil del entrenador no filtra su email | `web/VinculacionTest.atletaVeAsuEntrenador` |
| El historial de exatletas no trae id, edad ni peso | `web/VinculacionTest.elHistorialEsMinimo` |
| El listado del admin no trae hashes ni admins | `web/AdminTest.elListadoNoTraeAdmins` |

---

## RS10 — Impacto inmediato de cambios de autorización (ASVS v5 8.3.2)

**Cómo está implementado.** Dos mecanismos independientes, ninguno de los cuales cachea
permisos.

1. **`security/FiltroSesionVigente.java`** corre **antes** del `AuthorizationFilter` y vuelve
   a leer el usuario de la base en cada request. Si el usuario fue eliminado, invalida la
   `HttpSession` y responde 401. Si existe, reconstruye los *authorities* desde la fila
   actual, así que la sesión HTTP no funciona como caché de permisos.
2. **La pérdida de acceso por desvinculación no depende de la sesión**: las consultas de
   rutinas y ejecuciones exigen el vínculo activo en su propio `where`. El primer request
   posterior al cierre del vínculo ya no devuelve nada.

Además **no se usa JWT**, justamente por este requisito: un token firmado sigue siendo válido
hasta que expira, así que un cambio de autorización no tendría efecto inmediato sin una lista
de revocación (que es, otra vez, estado en el servidor).

Las dos operaciones que cambian autorización son **transaccionales**: `aceptarSolicitud`
(crea el vínculo y cancela las demás pendientes del atleta en la misma transacción) y
`eliminar` (limpia los datos dependientes y borra el usuario).

| Dónde | Qué hace |
|---|---|
| `security/FiltroSesionVigente.java` | Recarga el principal desde la base en cada request |
| `config/SecurityConfig.java` | `addFilterBefore(filtroSesionVigente, AuthorizationFilter.class)` |
| `repository/RutinaRepository.java`, `EjecucionRepository.java` | `exists` de vínculo activo dentro de cada consulta |
| `service/ServicioVinculacion.java` | `desvincularComoAtleta`, `desvincularComoEntrenador`, `aceptarSolicitud` (`@Transactional`) |
| `service/ServicioAdmin.java` | `eliminar` (`@Transactional`) y `expirarSesionesDe` |

**Nota sobre el `SessionRegistry`.** Al eliminar un usuario, `expirarSesionesDe` marca sus
sesiones registradas como expiradas, pero **el mecanismo que realmente corta el acceso es el
filtro**. No se puede invalidar la `HttpSession` de otro usuario desde afuera sin un almacén
de sesiones compartido, así que el registro queda como información y el filtro como la
garantía.

**Tests**

| Caso | Test |
|---|---|
| El atleta desvincula y el entrenador pierde acceso al perfil | `web/VinculacionTest.elAtletaDesvinculaYElEntrenadorPierdeAcceso` |
| El entrenador desvincula, con efecto inmediato | `web/VinculacionTest.elEntrenadorDesvincula` |
| Tras desvincular, el entrenador pierde las rutinas | `web/RutinasTest.alDesvincularPierdeAccesoALasRutinas` |
| Tras desvincular, el entrenador pierde el historial | `web/EjecucionesTest.alDesvincularPierdeAccesoAlHistorial` |
| Usuario eliminado por el admin → sesión inválida | `web/AdminTest.elUsuarioEliminadoPierdeLaSesion` |
| Al aceptar, las demás pendientes se cancelan en la misma transacción | `web/VinculacionTest.aceptarCancelaLasDemasPendientes` |

---

## RS12 — Logs de autorización

**Cómo está implementado.** `security/AuditorAcceso.java` emite **una línea JSON por evento**
con `ts`, `evento`, `estado`, `usuarioId` (o `anonimo`), `ip`, `metodo`, `recurso` y `motivo`.

Tres eventos: `acceso_denegado`, `login_fallido` y `sesion_invalidada`. Se registran los 401
del entry point, los 403 del access denied handler y los 404 por recurso ajeno del handler
global.

**Nunca se loguean** contraseñas, cookies, tokens, cabeceras ni cuerpos de request. Del
request solo salen el método y el path, sin query string. El email del admin tampoco se
registra al crearlo.

| Dónde | Qué hace |
|---|---|
| `security/AuditorAcceso.java` | `denegado(...)`, `loginFallido(...)`, `sesionInvalidada(...)` |
| `security/PuntoEntradaNoAutenticado.java` | 401 sin sesión |
| `security/ManejadorAccesoDenegado.java` | 403 fuera de rol o CSRF inválido |
| `error/ManejadorGlobalErrores.java` | `noEncontrado(...)`: registra el 404 por recurso ajeno |
| `security/FiltroSesionVigente.java` | registra la sesión invalidada |
| `resources/logback-spring.xml` | logger `AUDITORIA` → `logs/auditoria.log`, patrón `%msg%n` |

**Decisión.** No se usa `logstash-logback-encoder`. El JSON se arma a mano con los parámetros
ya saneados y el appender escribe `%msg%n`: una dependencia menos y el formato queda explícito
y auditable en el código, al lado del saneado.

**Tests**

| Caso | Test |
|---|---|
| El acceso sin sesión se audita sin datos sensibles | `web/LoginSesionTest.elAccesoSinSesionSeAudita` |
| El login fallido se registra con el email, nunca con la contraseña | `web/LoginSesionTest.elLoginFallidoSeLogueaSinLaContrasena` |
| La sesión invalidada se audita sin email ni cookie | `web/AdminTest.laSesionInvalidadaSeAudita` |

---

## RS13 — Deny by default, incluidos los recursos estáticos

**Cómo está implementado.** La cadena de autorización termina en **`anyRequest().denyAll()`**,
no en `authenticated()`. Todo lo que se sirve está enumerado:

- **Lista blanca pública**: `GET /api/csrf`, `POST /api/auth/login`, `POST /api/auth/registro`.
- **API por rol**: `/api/admin/**` → `ADMIN`, `/api/entrenador/**` → `ENTRENADOR`,
  `/api/atleta/**` → `ATLETA`, `/api/perfil` y `/api/auth/yo` → autenticado.
- **Estáticos**: solo los archivos del build del frontend.

Para los estáticos se tomaron tres medidas:

1. El mapeo automático de Spring Boot (`/**` → `classpath:/static/`) está **desactivado**
   (`spring.web.resources.add-mappings: false`).
2. Los únicos patrones servibles se declaran a mano: `/assets/**` y `/favicon.ico`.
3. El `index.html` se entrega leyendo el archivo del classpath, no con un `forward`, y solo
   para la lista explícita de rutas del SPA. **No hay catch-all `/**`.**

**Hallazgo corregido durante el desarrollo.** Antes del punto 2, `GET /assets/` devolvía el
**listado del directorio**. El `ResolverSoloArchivos` de
`config/ConfiguracionRecursosEstaticos.java` ahora rechaza todo recurso que sea un directorio
o cuyo path termine en `/`. El test `EstaticosTest.noHayListadoDeDirectorios` fija la regresión.

| Dónde | Qué hace |
|---|---|
| `config/SecurityConfig.java` | `authorizeHttpRequests(...)` terminando en `denyAll()`, constante `RUTAS_SPA` |
| `config/ConfiguracionRecursosEstaticos.java` | Declara los patrones servibles y rechaza directorios |
| `config/ControladorSpa.java` | Entrega `index.html` solo para las rutas conocidas del SPA |
| `resources/application.yml` | `spring.web.resources.add-mappings: false` |

Resultado: `/actuator/**`, `/h2-console`, `/swagger-ui.html`, `/v3/api-docs`,
`/application.yml`, `/logback-spring.xml`, `/seguridad/contrasenas-comunes.txt`, `/.env`,
`/BOOT-INF/...` y cualquier path de API inexistente devuelven **403**, nunca un listado ni el
contenido.

**Decisión.** `anonymous()` está deshabilitado. El efecto buscado es que un request **sin
sesión** a un endpoint protegido dé **401** (el `AuthorizationFilter` no encuentra
`Authentication` y lanza `AuthenticationCredentialsNotFoundException`), mientras que un
**path no permitido** dé **403**. Es justo la distinción que pide el enunciado.

**Tests** — `web/EstaticosTest` (11 tests)

| Caso | Test |
|---|---|
| Un archivo que existe en un path no permitido no se sirve | `unPathNoPermitidoNoSeSirve` |
| Los archivos de configuración del classpath no se sirven | `laConfiguracionNoSeSirve` |
| Actuator, consolas y endpoints de debug denegados | `losEndpointsDeDebugEstanDenegados` |
| Un endpoint de API inexistente se deniega, con y sin sesión | `unEndpointDeApiInexistenteSeDeniega` |
| No hay listado de directorios | `noHayListadoDeDirectorios` |
| Las rutas del SPA sí devuelven el index | `lasRutasDelSpaDevuelvenElIndex` |
| Request sin sesión a endpoint protegido → 401 | `web/LoginSesionTest.sinSesionDevuelve401` |
| Atleta en endpoint de entrenador / de admin → 403 | `web/VinculacionTest.atletaEnEndpointDeEntrenador`, `atletaEnEndpointDeAdmin` |
| Entrenador en endpoint de atleta → 403 | `web/VinculacionTest.entrenadorEnEndpointDeAtleta` |
| Entrenador y admin fuera de su panel → 403 | `web/AdminTest.entrenadorNoEntra`, `elAdminNoUsaLosOtrosPaneles` |

---

## RS14 — Sin configuración por defecto

**Cómo está implementado.**

| Control | Dónde |
|---|---|
| Sin usuario ni contraseña generados por Spring Security: `formLogin`, `httpBasic` y `anonymous` deshabilitados; el `AuthenticationManager` se arma a mano | `config/SecurityConfig.java` |
| Admin solo por variables de entorno; **si faltan, no se crea ningún admin**; si la contraseña no cumple la política, la app no arranca | `config/CreadorAdminInicial.java` |
| Sin credenciales en el código ni en `application.yml` | `resources/application.yml` |
| Handler global único, mensajes genéricos en español, sin stack traces ni nombres de clase | `error/ManejadorGlobalErrores.java` |
| `include-stacktrace: never`, `include-message: never`, `include-exception: false`, whitelabel apagada | `resources/application.yml` |
| Actuator, consola H2 y Swagger no están en el classpath, y aun si estuvieran quedan denegados | `pom.xml`, `config/SecurityConfig.java` |
| CORS restringido a `${CORS_ORIGENES}` con lista explícita de métodos y cabeceras | `config/SecurityConfig.java` |
| CSP, `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy` | `config/SecurityConfig.java` |
| Cookie `SHERPASESSION`: `HttpOnly`, `SameSite=Lax`, `Secure` por `${COOKIE_SECURE}` | `resources/application.yml` |
| BCrypt con factor 12 (default de Spring: 10) | `config/SecurityConfig.java` |
| Seed de ejemplo solo bajo el perfil `dev` y con `sherpa.seed.habilitado=true` | `config/SeedDesarrollo.java`, `resources/application-dev.yml` |

**Cabecera CSP aplicada:**

```
default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:;
font-src 'self'; connect-src 'self'; form-action 'self'; frame-ancestors 'none';
base-uri 'self'; object-src 'none'
```

HSTS está deshabilitado porque en local no hay TLS; se activa junto con `COOKIE_SECURE=true`
al servir por HTTPS.

**Decisión.** La suite de tests baja el costo de BCrypt a 4 por velocidad
(`sherpa.seguridad.costo-bcrypt`). Para que ese atajo no se filtre a producción,
`ConfiguracionDeProduccionTest` falla si alguien cambia el default de 12 o si
`application.yml` sobreescribe la propiedad.

**Tests**

| Caso | Test |
|---|---|
| Sin variables de entorno no se crea ningún admin | `ContextoCargaTest.sinVariablesDeEntornoNoHayAdmin` |
| El costo de BCrypt por defecto es 12 | `security/ConfiguracionDeProduccionTest.elCostoPorDefectoEsDoce`, `elYmlDeProduccionNoBajaElCosto` |
| El perfil por defecto no define ninguna credencial literal | `security/ConfiguracionDeProduccionTest.elPerfilPorDefectoNoTraeCredenciales`, `elPerfilPorDefectoNoUsaElSeed` |
| El seed está apagado por defecto | `security/ConfiguracionDeProduccionTest.elSeedEstaApagadoPorDefecto` |
| Las respuestas de error no exponen stack traces | `web/LoginSesionTest.lasRespuestasDeErrorNoTraenStackTrace` |
| El 403 de un path denegado no expone nada interno | `web/EstaticosTest.elErrorDeUnPathDenegadoEsGenerico` |
| El 401 no ofrece basic auth | `web/LoginSesionTest.el401NoOfreceBasicAuth` |
| Las cabeceras de seguridad están presentes | `web/EstaticosTest.lasCabecerasDeSeguridadEstanPresentes` |

---

## RS15 — Tests unitarios y de integración

**173 tests** con JUnit 5, Spring Boot Test y MockMvc, concentrados en la lógica de negocio y
sobre todo en los controles de seguridad.

| Clase | Tests | Qué cubre |
|---|---|---|
| `web/VinculacionTest` | 29 | solicitudes, vínculos, IDOR, roles, RS10 |
| `web/AdminTest` | 20 | panel de admin, no crear admins, RS10 al eliminar |
| `web/EjecucionesTest` | 18 | ejecuciones, validación de series, visibilidad |
| `web/RutinasTest` | 16 | rutinas, IDOR, rutinas propias invisibles al entrenador |
| `web/RegistroTest` | 14 | registro, mass assignment, no registrar admin |
| `web/LoginSesionTest` | 15 | login, 401, logout, CSRF, RS12 |
| `service/PoliticaContrasenaTest` | 14 | política de contraseñas (unitario) |
| `web/PerfilTest` | 12 | perfil propio, mass assignment, edad |
| `web/EstaticosTest` | 11 | RS13 y cabeceras |
| `service/CalculadoraEdadTest` | 8 | cálculo de edad (unitario) |
| `security/SanitizadorLogTest` | 5 | RS17, saneado (unitario) |
| `security/ConfiguracionDeProduccionTest` | 5 | RS14, guardas del perfil por defecto |
| `web/CsrfFlujoRealTest` | 4 | flujo CSRF real cookie + cabecera |
| `ContextoCargaTest` | 2 | arranque, ausencia de admin por defecto |

Infraestructura: `support/TestIntegracion.java` (base común, limpia la base en un
`@BeforeEach` para que ningún test dependa del orden) y
`support/CapturadorDeAuditoria.java` (appender en memoria sobre el logger `AUDITORIA`).

**Cobertura de la lista de casos obligatorios del enunciado**

| Caso pedido | Test |
|---|---|
| Request sin sesión a endpoint protegido → 401 | `web/LoginSesionTest.sinSesionDevuelve401` |
| Atleta en endpoint de entrenador/admin y viceversa → 403 | `web/VinculacionTest.atletaEnEndpointDeEntrenador`, `atletaEnEndpointDeAdmin`, `entrenadorEnEndpointDeAtleta` |
| Atleta lee o completa la rutina de otro → denegado | `web/RutinasTest.noLeeRutinaAjena`, `noCompletaRutinaAjena` |
| Entrenador ve rutinas/ejecuciones de un atleta ajeno → denegado | `web/RutinasTest.noVeRutinasDeAtletaAjeno`, `web/EjecucionesTest.noVeHistorialDeAtletaAjeno` |
| Entrenador ve las rutinas propias de su atleta → denegado | `web/RutinasTest.noVeLasRutinasPropiasDelAtleta`, `web/EjecucionesTest.noVeEjecucionesDeRutinasPropias` |
| Tras desvincular, el entrenador pierde acceso en el request siguiente | `web/VinculacionTest.elAtletaDesvinculaYElEntrenadorPierdeAcceso`, `web/RutinasTest.alDesvincularPierdeAccesoALasRutinas`, `web/EjecucionesTest.alDesvincularPierdeAccesoAlHistorial` |
| Usuario eliminado por el admin queda con la sesión invalidada | `web/AdminTest.elUsuarioEliminadoPierdeLaSesion` |
| Enviar `rol` u otros campos no permitidos no tiene efecto | `web/RegistroTest.campoNoPermitidoEsRechazado`, `passwordHashNoSePuedeInyectar`, `web/PerfilTest.mandarRolNoTieneEfecto`, `mandarEmailNoTieneEfecto`, `mandarIdNoTieneEfecto`, `mandarPasswordHashNoTieneEfecto` |
| No se puede registrar un admin | `web/RegistroTest.noSePuedeRegistrarUnAdmin`, `rolInexistenteNoCreaNada`, `web/AdminTest.noPuedeCrearOtroAdmin` |
| Al aceptar, las demás pendientes pasan a cancelada | `web/VinculacionTest.aceptarCancelaLasDemasPendientes` |
| Un atleta con entrenador no puede enviar solicitudes | `web/VinculacionTest.conEntrenadorActivoNoPuedeSolicitar` |
| Los accesos denegados generan log sin datos sensibles | `web/LoginSesionTest.elAccesoSinSesionSeAudita`, `elLoginFallidoSeLogueaSinLaContrasena`, `web/AdminTest.laSesionInvalidadaSeAudita` |
| Las respuestas de error no exponen stack traces | `web/LoginSesionTest.lasRespuestasDeErrorNoTraenStackTrace`, `web/EstaticosTest.elErrorDeUnPathDenegadoEsGenerico` |
| Un path estático no permitido no se sirve | `web/EstaticosTest.unPathNoPermitidoNoSeSirve`, `laConfiguracionNoSeSirve` |
| Tests unitarios de la política de contraseñas y del cálculo de edad | `service/PoliticaContrasenaTest` (14), `service/CalculadoraEdadTest` (8) |

---

## RS17 — Protección de logs

**Cómo está implementado.** `security/SanitizadorLog.limpiar()` procesa **todo** dato de
origen externo antes de que llegue al log:

- Reemplaza `\r`, `\n` y `\t` por un espacio, lo que impide inyectar una entrada de log falsa.
- Descarta el resto de los caracteres de control, incluidas las secuencias ANSI de escape.
- Escapa `"` y `\` para que no se pueda romper la estructura JSON del evento.
- Trunca a 200 caracteres.

Los archivos se escriben en `${LOG_DIR:-logs}`, **fuera** de `resources/static`, que es el
único directorio servido, y `logs/` está en `.gitignore`. El formato es estructurado: una
línea JSON por evento de auditoría. No se loguea SQL con parámetros
(`org.hibernate.SQL` en `WARN`), ni cuerpos de request, ni cabeceras.

| Dónde | Qué hace |
|---|---|
| `security/SanitizadorLog.java` | `limpiar(...)` |
| `security/AuditorAcceso.java` | Pasa cada valor por el saneador antes de armar el JSON |
| `resources/logback-spring.xml` | Appenders a `${LOG_DIR}`, logger `AUDITORIA` con `%msg%n` |
| `resources/application.yml` | Niveles de log: sin SQL ni parámetros |
| `.gitignore` | `logs/`, `*.log` |

**Tests**

| Caso | Test |
|---|---|
| Elimina saltos de línea | `security/SanitizadorLogTest.eliminaSaltosDeLinea` |
| Escapa comillas | `security/SanitizadorLogTest.escapaComillas` |
| Elimina caracteres de control y ANSI | `security/SanitizadorLogTest.eliminaCaracteresDeControl` |
| Trunca valores largos | `security/SanitizadorLogTest.truncaValoresLargos` |
| Un email con `\n` no puede inyectar una entrada falsa (el evento sigue siendo uno y es el real) | `web/LoginSesionTest.noSePuedeInyectarEnElLog` |

---

## Base de seguridad general

### Mass assignment

Tres barreras, en capas:

1. **DTOs de entrada con lista blanca de campos.** Son records que solo declaran lo editable.
   `RegistroRequest` y `CrearUsuarioAdminRequest` usan el enum **`RolRegistro`, que no
   incluye `ADMIN`**: un body con `"rol":"ADMIN"` falla al deserializar y devuelve 400 sin
   llegar al servicio. Es más fuerte que validar y rechazar, porque el valor no es ni
   representable.
2. **Jackson en modo estricto** (`fail-on-unknown-properties: true`): un body con un campo
   que el DTO no declara (`id`, `passwordHash`, `origen`, `idAtleta`) devuelve 400 en lugar de
   ignorarse en silencio.
3. **`updatable = false` en la entidad** para `email`, `rol`, y todos los campos de `Rutina`,
   `Ejecucion` y `SerieEjecutada`. Ni un bug en el servicio podría emitir un `UPDATE` sobre
   esas columnas.

La única escritura que existe sobre `entrenador_creador_id` es
`RutinaRepository.soltarEntrenadorCreador`, usada al eliminar un entrenador para que el
atleta conserve sus rutinas.

### Validación de entradas

Bean Validation en todos los DTOs de entrada, con mensajes en español. Las validaciones de
negocio que no se pueden expresar con anotaciones viven en los servicios:
`CalculadoraEdad.validarFechaNacimiento`, `ServicioUsuarios.validarCamposPorRol`,
`PoliticaContrasena.validar` y la validación de las series de una ejecución contra los
ejercicios de la rutina.

Las respuestas de error de validación informan **qué campo** falla, nunca el valor recibido
(`web/RegistroTest.informaCamposInvalidos`).

### Sesiones y CSRF

- **Sesiones del lado del servidor**, no JWT (ver RS10).
- Cookie `SHERPASESSION`: `HttpOnly`, `SameSite=Lax`, `Secure` configurable.
- **Regeneración del id de sesión al loguearse** (`ChangeSessionIdAuthenticationStrategy`),
  verificada en `web/LoginSesionTest.elLoginRegeneraElIdDeSesion`.
- **Logout** que invalida la sesión y borra las cookies
  (`web/LoginSesionTest.elLogoutInvalidaLaSesion`).
- **CSRF compatible con SPA**: cookie `XSRF-TOKEN` legible por JS y cabecera `X-XSRF-TOKEN`.
  La cookie es legible a propósito, porque el cliente tiene que reenviarla en la cabecera;
  eso no la debilita, ya que un atacante de otro origen no puede leerla. Se desactiva el
  envoltorio anti-BREACH (`setCsrfRequestAttributeName(null)`) porque el SPA nunca renderiza
  el token en el HTML. Cubierto por `web/CsrfFlujoRealTest` (4 tests).

### Transacciones

`@Transactional` en todas las operaciones que tocan más de una entidad: `aceptarSolicitud`,
`desvincularComoAtleta`, `desvincularComoEntrenador`, `registrar` (ejecución),
`crearParaAtleta` y `eliminar` (admin).

---

## El frontend no es un control de seguridad

El SPA oculta lo que no corresponde a cada rol (`components/Layout.tsx` muestra solo el menú
del rol, `components/RutaProtegida.tsx` redirige si el rol no coincide), pero **eso es solo
experiencia de usuario**. Saltearse esas guardas a mano no da acceso a ningún dato: toda la
autorización se aplica en el backend, primero en la cadena de filtros y de nuevo en cada
consulta.

El cliente HTTP (`frontend/src/api/cliente.ts`) no guarda datos del usuario en
`localStorage`: la sesión vive en el servidor, así que cualquier cambio de autorización se
refleja en el request siguiente. Ante un 401 limpia el estado local y manda al login.
