# Controles de seguridad — Los Sherpa

Mapeo de cada requerimiento de seguridad del sprint con dónde está implementado y qué tests
lo cubren. En el código, cada punto está marcado con un comentario (`// RS04`, `// RS10`, …).

Las rutas de clases son relativas a `backend/src/main/java/com/lossherpa/` y las de tests a
`backend/src/test/java/com/lossherpa/`.

**Estado de la suite: 269 tests, todos en verde** (`cd backend && ./mvnw test`).

---

## Resumen

| RS | Requisito | Estado | Tests principales |
|---|---|---|---|
| RS01 | Control de acceso centralizado (PEP) | ✅ | `security/PoliticaAccesoTest`, `security/DefensaEnProfundidadTest` |
| RS02 | Deny by default para requests no contempladas | ✅ | `web/DenegacionPorDefectoTest` |
| RS03 | Modelo híbrido RBAC + ABAC | ✅ | `security/PoliticaAccesoTest`, tests ABAC de `web/RutinasTest`, `EjecucionesTest`, `VinculacionTest` |
| RS04 | Control de acceso a nivel de dato (ASVS 8.2.2) | ✅ | ver RS04 |
| RS05 | Controles solo en el servidor | ✅ | `security/PoliticaAccesoTest.ningunHandlerRecibeRolNiIdentidad`, `security/IdentidadDesdeLaSesionTest` |
| RS06 | Control por campo / mass assignment (ASVS 8.2.3) | ✅ | `web/CamposPorRolTest`, `web/RegistroTest`, `web/PerfilTest` |
| RS07 | No exposición de IDs | ✅ | ver RS07 |
| RS08 | Control de acceso basado en estado | ✅ | `web/RutinasTest`, `web/VinculacionTest` (ver RS08) |
| RS09 | ❔ Sin definición en el repo | a confirmar con el equipo | — |
| RS10 | Impacto inmediato de cambios de autorización (ASVS 8.3.2) | ✅ | ver RS10 |
| RS11 | Métricas financieras | no aplica: no hay facturación en esta versión | — |
| RS12 | Logs de autorización | ✅ | ver RS12 |
| RS13 | Deny by default, incluidos los estáticos | ✅ | `web/EstaticosTest` |
| RS14 | Sin configuración por defecto | ✅ | ver RS14 |
| RS15 | Tests unitarios y de integración | ✅ | 269 tests |
| RS16 | Toda request pasa por el control de acceso | ✅ | `security/CoberturaControlAccesoTest`, `security/IdentidadDesdeLaSesionTest` |
| RS17 | Protección de logs | ✅ | `security/SanitizadorLogTest` |
| RS18 | Deny by default ante errores (fail closed) | ✅ | `security/FallaCerradaTest`, `security/FallaCerradaRealTest`, `security/PoliticaAccesoFallaCerradaTest` |

---

## Política de autorización

**Fuente de verdad: `security/PoliticaAcceso.java`.** Esta tabla es la misma lista, una fila
por endpoint (método + patrón exacto). Lo que no está acá se deniega (RS02).
`security/CoberturaControlAccesoTest` falla si un handler real no tiene regla o si una regla
no corresponde a ningún handler, así que la tabla y el código no se pueden separar sin
romper el build.

| Método | Path | Rol | Atributo (ABAC) | Denegación |
|---|---|---|---|---|
| GET | `/api/csrf` | público | — | — |
| POST | `/api/auth/login` | público | — | — |
| POST | `/api/auth/registro` | público | — | — |
| GET | `/api/auth/yo` | autenticado (cualquier rol) | propio | 401 sin sesión |
| GET | `/api/perfil` | autenticado (cualquier rol) | propio | 401 sin sesión |
| PUT | `/api/perfil` | autenticado (cualquier rol) | propio | 401 sin sesión |
| PUT | `/api/perfil/preferencias` | autenticado (cualquier rol) | propio | 401 sin sesión |
| GET | `/api/atleta/rutinas` | ATLETA | dueño | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/api/atleta/rutinas/{idRutina}` | ATLETA | dueño | 401 sin sesión · 403 otro rol · 404 sin relación |
| POST | `/api/atleta/rutinas` | ATLETA | dueño | 401 sin sesión · 403 otro rol · 404 sin relación |
| POST | `/api/atleta/rutinas/{idRutina}/ejecuciones` | ATLETA | dueño | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/api/atleta/ejecuciones` | ATLETA | dueño | 401 sin sesión · 403 otro rol · 404 sin relación |
| DELETE | `/api/atleta/ejecuciones/{idEjecucion}` | ATLETA | dueño | 401 sin sesión · 403 otro rol · 404 ajena/inexistente |
| DELETE | `/api/atleta/ejecuciones` | ATLETA | dueño | 401 sin sesión · 403 otro rol |
| GET | `/api/atleta/entrenadores` | ATLETA | — | 401 sin sesión · 403 otro rol |
| GET | `/api/atleta/solicitudes` | ATLETA | dueño | 401 sin sesión · 403 otro rol · 404 sin relación |
| POST | `/api/atleta/solicitudes` | ATLETA | dueño | 401 sin sesión · 403 otro rol · 404 sin relación |
| POST | `/api/atleta/solicitudes/{idSolicitud}/cancelar` | ATLETA | dueño | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/api/atleta/entrenador` | ATLETA | vínculo vigente | 401 sin sesión · 403 otro rol · 404 sin relación |
| DELETE | `/api/atleta/vinculo` | ATLETA | vínculo vigente | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/api/entrenador/atletas` | ENTRENADOR | vínculo vigente | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/api/entrenador/exatletas` | ENTRENADOR | dueño | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/api/entrenador/solicitudes` | ENTRENADOR | destinatario | 401 sin sesión · 403 otro rol · 404 sin relación |
| POST | `/api/entrenador/solicitudes/{idSolicitud}/aceptar` | ENTRENADOR | destinatario | 401 sin sesión · 403 otro rol · 404 sin relación |
| POST | `/api/entrenador/solicitudes/{idSolicitud}/rechazar` | ENTRENADOR | destinatario | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/api/entrenador/atletas/{idAtleta}` | ENTRENADOR | vínculo vigente | 401 sin sesión · 403 otro rol · 404 sin relación |
| DELETE | `/api/entrenador/atletas/{idAtleta}/vinculo` | ENTRENADOR | vínculo vigente | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/api/entrenador/atletas/{idAtleta}/rutinas` | ENTRENADOR | vínculo vigente + asignada por mí | 401 sin sesión · 403 otro rol · 404 sin relación |
| POST | `/api/entrenador/atletas/{idAtleta}/rutinas` | ENTRENADOR | vínculo vigente | 401 sin sesión · 403 otro rol · 404 sin relación |
| PUT | `/api/entrenador/atletas/{idAtleta}/rutinas/{idRutina}` | ENTRENADOR | vínculo vigente + asignada por mí | 401 sin sesión · 403 otro rol · 404 sin relación |
| DELETE | `/api/entrenador/atletas/{idAtleta}/rutinas/{idRutina}` | ENTRENADOR | vínculo vigente + asignada por mí | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/api/entrenador/atletas/{idAtleta}/ejecuciones` | ENTRENADOR | vínculo vigente + asignada por mí | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/api/admin/usuarios` | ADMIN | gestionable (no ADMIN) | 401 sin sesión · 403 otro rol · 404 sin relación |
| POST | `/api/admin/usuarios` | ADMIN | — | 401 sin sesión · 403 otro rol |
| GET | `/api/admin/usuarios/{idUsuario}` | ADMIN | gestionable (no ADMIN) | 401 sin sesión · 403 otro rol · 404 sin relación |
| PUT | `/api/admin/usuarios/{idUsuario}` | ADMIN | gestionable (no ADMIN) | 401 sin sesión · 403 otro rol · 404 sin relación |
| DELETE | `/api/admin/usuarios/{idUsuario}` | ADMIN | gestionable (no ADMIN) | 401 sin sesión · 403 otro rol · 404 sin relación |
| GET | `/`, `/index.html`, `/login`, `/registro`, `/entrenador`, `/entrenador/**`, `/atleta`, `/atleta/**`, `/admin`, `/admin/**`, `/assets/**`, `/favicon.ico` | público | — | — |
| todos | `/error` | nadie | — | 403 siempre |

Cómo leer "Denegación": **401** sin sesión (`PuntoEntradaNoAutenticado`), **403** rol
equivocado (`ManejadorAccesoDenegado`), **404** si no se cumple el atributo de relación (lo
aplica la consulta, ver RS04). Las tres respuestas quedan en el log de auditoría (RS12).

`POST /api/auth/logout` no figura: lo atiende el `LogoutFilter` de Spring Security antes de
la autorización y no llega a ningún handler ni servicio. Exige CSRF y sin sesión responde
204 igual (es idempotente).

---

## RF7 — Preferencias y teléfono de contacto (RS06)

- `PUT /api/perfil/preferencias` (`ActualizarPreferenciasRequest`) solo acepta `tema`
  (`CLARO`/`OSCURO`/`SISTEMA`). Cualquier otro campo (`rol`, `email`, `telefono`) o un tema
  inválido da 400. El tema viaja en `PerfilResponse` y `SesionResponse`.
- `telefono` es opcional (≤ 30, solo dígitos, espacios, `+`, `()` y `-`), editable por
  `PUT /api/perfil`; null o vacío lo borra.
- **Matriz de campos (SH-27), teléfono:** lo ven el propio usuario (`PerfilResponse`) y la
  contraparte con vínculo vigente (`PerfilPublicoResponse.deAtletaVinculado` /
  `deEntrenadorVinculado`: mi entrenador, mis atletas, detalle de atleta). No viaja en la
  búsqueda de entrenadores, en las solicitudes (enviadas/recibidas) ni en el historial de
  exatletas.
- El tema se aplica con el atributo `data-tema` en `<html>` (CSP `style-src 'self'`).
- Tests: `web/PerfilTest` (sección RF7).

---

## RS01 — Control de acceso centralizado (Policy Enforcement Point)

**Cómo está implementado.** Hay **un punto de decisión** y **dos puntos de aplicación**:

- **PDP: `security/PoliticaAcceso`.** Declara la tabla de arriba y decide con
  `exigir(regla, autenticacion)`: deja pasar, lanza `AuthenticationCredentialsNotFoundException`
  (401) o `AccesoDenegadoPorPolitica` (403, con el motivo para la auditoría).
- **PEP 1, filtro por URL.** `SecurityConfig` recorre la política y registra cada regla con
  su método y su patrón en `authorizeHttpRequests`, delegando la decisión en la política.
  La configuración no tiene ningún `hasRole` escrito a mano.
- **PEP 2, interceptor de MVC.** `InterceptorPoliticaAcceso` vuelve a consultar la política
  sobre el **patrón del handler que eligió MVC** (`BEST_MATCHING_PATTERN_ATTRIBUTE`), no sobre
  la URL. Si la regla de URL se borra o se abre por error, o si la URL y el handler no
  coinciden, la request igual se deniega antes del controlador. Un handler sin regla se
  deniega (fail closed).

Además, cada servicio sigue llamando a `ServicioUsuarios.autenticadoConRol(...)` como
tercera barrera.

| Dónde | Qué hace |
|---|---|
| `security/PoliticaAcceso.java` | Tabla de reglas (método + patrón + rol + atributo) y la decisión `exigir(...)` |
| `config/SecurityConfig.java` | Registra cada regla de la política en el filtro; termina en `DenegacionPorDefecto` (RS02) |
| `security/InterceptorPoliticaAcceso.java` | Segunda decisión sobre el handler elegido por MVC |
| `config/ConfiguracionPoliticaAcceso.java` | Registra el interceptor para todos los handlers (controladores, estáticos, `/error`) |
| `security/AccesoDenegadoPorPolitica.java` | 403 con el motivo real para el log |
| `security/ManejadorAccesoDenegado.java`, `error/ManejadorGlobalErrores.java` | Auditan el motivo que dio la política (filtro y MVC respectivamente) |

**Decisión: `AuthorizationManager` con política declarada (opción a), no `@PreAuthorize`.**
Con `@PreAuthorize` el rol de cada operación quedaría repartido entre anotaciones en cada
controlador y las reglas de URL. Con la tabla, el rol se escribe **una sola vez**, los dos
PEP leen lo mismo y el test de RS16 la recorre directamente. La segunda capa decide sobre el
handler y no sobre la URL, que es justamente lo que una regla de URL mal escrita no ve.

**Decisión: la política lanza su propia excepción.** En Spring Security 6.3 el
`AuthorizationFilter` descarta la decisión y lanza un `AccessDeniedException("Access Denied")`
genérico: el motivo se perdería. `AccesoDenegadoPorPolitica` lo lleva hasta el log; al
cliente le llega siempre el mismo 403 genérico.

**Decisión: `autenticadoConRol` se mantiene.** No decide política (eso es la tabla): verifica
un invariante del servicio ("las operaciones de atleta operan sobre un atleta"). Si alguna
vez falla es un bug de la configuración, y responde 403 auditado igual.

**Tests**

| Caso | Test |
|---|---|
| Sin sesión, todo endpoint de rol → 401 | `security/PoliticaAccesoTest.sinSesionEs401` |
| Rol fuera de su grupo → 403 con una línea de auditoría por intento | `security/PoliticaAccesoTest.rolEquivocadoEs403YSeAudita` |
| El rol correcto entra | `security/PoliticaAccesoTest.elRolCorrectoEntra` |
| La política no tiene reglas duplicadas | `security/PoliticaAccesoTest.laPoliticaNoTieneReglasDuplicadas` |
| Sin la regla de URL de admin, el atleta igual recibe 403 de la segunda capa | `security/DefensaEnProfundidadTest.sinReglaDeUrlElAtletaIgualEsDenegado` |
| Sin la regla de URL, sin sesión → 401 de la segunda capa | `security/DefensaEnProfundidadTest.sinReglaDeUrlSinSesionEs401` |
| La segunda capa no le quita acceso al rol correcto | `security/DefensaEnProfundidadTest.elAdminSigueEntrando` |
| Matriz completa regla × sesión | `security/CoberturaControlAccesoTest.matrizCompletaDeAcceso` (RS16) |

---

## RS02 — Deny by default para requests no contempladas

**Cómo está implementado.** Las reglas son **por método y patrón exacto**, una por endpoint
(ver "Política de autorización"). La última regla de la cadena es
`anyRequest().access(DenegacionPorDefecto)`, que **nunca concede**: toda request que no
coincide con una regla (path inexistente bajo un prefijo válido, método no declarado, `HEAD`,
`OPTIONS` fuera de un preflight CORS) la deniega la capa de autorización y queda auditada con
motivo `request no contemplada`. Ya no depende de que MVC no encuentre un handler.

**Antes del cambio** (reglas por prefijo y sin método), `web/DenegacionPorDefectoTest` daba:

| Request | Antes | Ahora |
|---|---|---|
| `GET /api/atleta/no-existe` (atleta) | 404 de MVC, sin auditoría | 403 + auditoría |
| `PATCH` / `DELETE /api/perfil`, `PUT /api/entrenador/atletas/{id}/rutinas` | **500** (el 405 de MVC caía en el catch-all) | 403 + auditoría |
| `POST /login` (ruta del SPA) | **500** | 403 |
| `HEAD /api/atleta/rutinas` | 200 | 403 |
| `OPTIONS /api/atleta/rutinas` sin CORS | 200 | 403 |
| `TRACE /api/perfil` | 400 del firewall, sin auditoría | 400 + auditoría |

**Decisión: una request no contemplada de un usuario autenticado devuelve 403.** Es
consistente con RS13: la ruta no existe para nadie, así que no hay recurso que enumerar. Sin
sesión, dentro de las zonas que exigen sesión (`/api/atleta/**`, `/api/entrenador/**`,
`/api/admin/**`, `/api/perfil/**`, `/api/auth/**`) responde **401**: primero hay que
autenticarse. Fuera de esas zonas (`/api/no-existe`, `/actuator`) sigue siendo 403, como
fija `web/EstaticosTest`.

**`TRACE`.** Lo rechaza el `StrictHttpFirewall` de Spring Security **antes** de autorizar
(no está en su lista de métodos), con 400. Antes no quedaba rastro; ahora el
`RequestRejectedHandler` de `SecurityConfig` lo audita como `request rechazada por el
firewall` y responde el JSON genérico. Lo mismo vale para paths con `..`, `;` o `//`. Además,
Tomcat trae `allowTrace=false` por defecto.

**`/**` que quedan.** Bajo `/api/` no queda ninguno. Los únicos comodines son rutas del SPA
(`/atleta/**`, `/entrenador/**`, `/admin/**`) y `/assets/**`: solo `GET`, y solo devuelven el
`index.html` o un archivo del build (RS13).

**Método no declarado (RS18).** Con RS02 ninguna request llega a MVC con un método no
declarado: la deniega antes el filtro de seguridad. Si llegara, `ManejadorGlobalErrores` la
resuelve por el catch-all de RS18 (500 genérico, sin datos), así que también falla cerrado.

| Dónde | Qué hace |
|---|---|
| `security/DenegacionPorDefecto.java` | Última regla: 401 sin sesión en zona protegida, 403 `request no contemplada` en el resto |
| `config/SecurityConfig.java` | Reglas exactas desde la política + `anyRequest().access(denegacionPorDefecto)`; `requestRejectedHandler` |
| `security/ManejadorAccesoDenegado.java` | Audita el motivo (`request no contemplada`, rol, CSRF) |

**Tests** — `web/DenegacionPorDefectoTest` (11 tests)

| Caso | Test |
|---|---|
| Path inexistente bajo prefijo válido → 403 + auditoría | `pathInexistenteBajoPrefijoValido` |
| `PATCH` / `DELETE /api/perfil` → 403 + auditoría | `metodoNoDeclaradoSobrePerfil` |
| `PUT` no declarado del entrenador → 403 | `putNoDeclaradoDelEntrenador` |
| Sin sesión, las mismas requests → 401 | `sinSesionEs401` |
| `TRACE` → 400 del firewall, auditado | `traceLoRechazaElFirewall` |
| Paths con `..` o `;` → 400 del firewall, auditado | `pathsManipuladosLosRechazaElFirewall` |
| `HEAD` no declarado → 403 | `headNoDeclaradoSeDeniega` |
| `OPTIONS` sin CORS → 403; el preflight del origen permitido sigue andando | `optionsSinCorsSeDeniega`, `elPreflightCorsSigueFuncionando` |
| Ruta del SPA con otro método → 403 (antes 500) | `rutaDelSpaConOtroMetodoSeDeniega` |
| Regresión de endpoints legítimos | `losEndpointsLegitimosSiguenIgual` |

---

## RS03 — Modelo híbrido RBAC + ABAC

**Cómo está implementado.** Dos capas, en lugares distintos a propósito:

1. **RBAC en la política.** El rol de cada endpoint es la columna "Rol" de la tabla. Lo
   aplican los dos PEP de RS01.
2. **ABAC en la consulta.** La columna "Atributo" nombra el predicado de relación de cada
   endpoint. Los predicados están definidos **en un solo lugar** (el enum
   `PoliticaAcceso.Atributo`, que documenta qué consulta implementa cada uno), pero se siguen
   aplicando **dentro del `where`** del repositorio: no se pasó a "buscar y después chequear".
   Si no se cumplen, la consulta no devuelve filas y la respuesta es 404 (RS04).

| Atributo | Predicado | Dónde se aplica |
|---|---|---|
| `PROPIO` | el recurso es el usuario de la sesión | `UsuarioActual.id()` en `ServicioUsuarios`, `ServicioPerfil`, `ServicioAutenticacion` |
| `DUENO` | `atleta.id = :yo` (o `entrenador.id = :yo` en sus vínculos finalizados) | `RutinaRepository.buscarDeAtleta`, `listarPorAtleta`; `EjecucionRepository.buscarDeAtleta`, `listarPorAtleta`; `SolicitudVinculacionRepository.buscarDeAtleta`; `VinculoRepository.listarFinalizadosPorEntrenador` |
| `DESTINATARIO` | la solicitud está dirigida al entrenador de la sesión | `SolicitudVinculacionRepository.buscarDeEntrenador` |
| `VINCULO_VIGENTE` | vínculo entrenador↔atleta con `fechaFin is null` | `VinculoRepository.buscarActivoPorEntrenadorYAtleta`, `ServicioVinculacion.vinculoActivoCon` |
| `ASIGNADA_POR_MI` | vínculo vigente + `origen = ASIGNADA` + `entrenadorCreador = :yo` | `RutinaRepository.listarAsignadasPorEntrenador`, `EjecucionRepository.listarAsignadasPorEntrenador` |
| `GESTIONABLE` | el usuario no es ADMIN | `UsuarioRepository.buscarGestionablePorAdmin`, `buscarParaAdmin` |

**Tests.** RBAC: `security/PoliticaAccesoTest` y la matriz de `CoberturaControlAccesoTest`.
ABAC (reutilizados, no duplicados): `web/VinculacionTest.noVeAtletaAjeno`,
`noVeAtletaSinVinculo`, `elAtletaDesvinculaYElEntrenadorPierdeAcceso`;
`web/RutinasTest.noVeRutinasDeAtletaAjeno`, `alDesvincularPierdeAccesoALasRutinas`;
`web/EjecucionesTest.noVeHistorialDeAtletaAjeno`, `alDesvincularPierdeAccesoAlHistorial`.

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
| `repository/EjecucionRepository.java` | `buscarDeAtleta`, `listarPorAtleta`, `listarAsignadasPorEntrenador` |
| `repository/SolicitudVinculacionRepository.java` | `buscarDeAtleta`, `buscarDeEntrenador` |
| `repository/VinculoRepository.java` | `buscarActivoPorAtleta`, `buscarActivoPorEntrenadorYAtleta` |
| `service/ServicioVinculacion.java` | `vinculoActivoCon(...)`: punto único donde el entrenador prueba su relación vigente |
| `service/ServicioEjecuciones.java` | `registrar(...)`: los `idEjercicio` del cliente se validan contra los ejercicios de **esa** rutina; `eliminar(...)` y `eliminarTodoMiHistorial()` borran solo datos del atleta autenticado |
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
| Atleta borra una ejecución propia; ya no aparece para atleta ni entrenador | `web/EjecucionesTest.eliminaSuEjecucion` |
| Atleta intenta borrar ejecución ajena y recibe 404 auditado | `web/EjecucionesTest.noEliminaEjecucionAjena` |
| Borrado total solo afecta el historial del atleta autenticado | `web/EjecucionesTest.eliminaTodoSuHistorialSinAfectarAOtros` |
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

## RS05 — Controles solo en el servidor

**Cómo está implementado.** Ningún control confía en el cliente:

- El rol con el que decide la política sale del principal que `FiltroSesionVigente`
  reconstruye **desde la base** en cada request, nunca de un parámetro, una cabecera ni de
  lo que el frontend cree que es el usuario.
- Ningún handler recibe el rol, el id del usuario actual ni un flag del tipo `esAdmin`. Lo
  verifican dos tests por reflexión sobre los handlers reales.
- El frontend solo adapta la experiencia (ver "El frontend no es un control de seguridad").

Excepciones justificadas, declaradas en los tests: en `/api/admin/**` el `idUsuario` y el
`rol` son del usuario **gestionado** (no del admin), y en `POST /api/auth/registro` el `rol`
es el de la cuenta nueva (`RolRegistro` no incluye `ADMIN`).

**Tests**

| Caso | Test |
|---|---|
| Ningún parámetro se llama `rol`, `esAdmin`, `idUsuarioActual`, … | `security/PoliticaAccesoTest.ningunHandlerRecibeRolNiIdentidad` |
| Ningún parámetro ni campo de body representa al usuario actual | `security/IdentidadDesdeLaSesionTest.ningunDatoDelClienteEsLaIdentidad` |

---

## RS06 — Control de acceso a nivel de campo (ASVS v5 8.2.3)

**Qué cubre.** Dos lados de BOPLA (Broken Object Property Level Authorization):

- **Escritura (CWE-915, mass assignment).** Un usuario no puede modificar campos que no le
  corresponden (rol, email, dueño, origen de una rutina, campos de otro rol), ni siquiera
  agregándolos al JSON.
- **Lectura (CWE-213, exposición excesiva).** Cada respuesta incluye **solo los campos que
  ese consumidor puede ver**.

### Cómo está implementado

**Escritura**, en capas:

1. **DTOs de entrada con lista blanca de campos** (`web/dto/in/`). Son records que solo
   declaran lo editable. `RegistroRequest` y `CrearUsuarioAdminRequest` usan el enum
   **`RolRegistro`, que no incluye `ADMIN`**: un body con `"rol":"ADMIN"` falla al
   deserializar y devuelve 400 sin llegar al servicio. Es más fuerte que validar y rechazar,
   porque el valor no es ni representable.
2. **Jackson en modo estricto** (`fail-on-unknown-properties: true`): un body con un campo
   que el DTO no declara (`id`, `passwordHash`, `origen`, `idAtleta`) devuelve 400 en lugar
   de ignorarse en silencio. Vale también para los objetos anidados (ejercicios y series).
3. **`updatable = false` en la entidad** para `email`, `rol`, y todos los campos de `Rutina`,
   `EjercicioRutina`, `Ejecucion` y `SerieEjecutada`. Ni un bug en el servicio podría emitir
   un `UPDATE` sobre esas columnas.
4. **Campos por rol validados contra el registro editado**
   (`ServicioUsuarios.validarCamposPorRol`): `pesoKg` solo para un atleta y `aniosServicio`
   solo para un entrenador. Se evalúa el rol **del usuario editado, no el de quien edita**, y
   por eso el admin tampoco puede ponerle peso a un entrenador (lo usan `ServicioPerfil`,
   `ServicioAdmin.actualizar` y el alta, vía `ServicioAutenticacion.registrar`).

La única escritura sobre `entrenador_creador_id` es `RutinaRepository.soltarEntrenadorCreador`,
usada al eliminar un entrenador para que el atleta conserve sus rutinas.

**Lectura.** Nunca se serializa una entidad: hay un DTO de salida por cada vista
(`web/dto/out/`), y `PerfilPublicoResponse` tiene una fábrica por rol (`deEntrenador`,
`deAtleta`) para que cada una exponga solo los campos de ese rol. Los campos nulos no se
serializan (`default-property-inclusion: non_null`), así que el `pesoKg` de un atleta no
aparece en el perfil de un entrenador ni al revés.

### Matriz de campos

Notación: **L** lee · **E** escribe (se puede modificar después de creado) · **C** se fija al
crear y no se puede cambiar · **–** nada. "Propio" es el usuario sobre su propio registro.
"Gestionable" es un atleta o un entrenador (el admin no ve ni edita a otros admins).

**`Usuario`**

| Campo | Propio | Entrenador con vínculo vigente (ve a su atleta) | Atleta (ve a un entrenador) | Entrenador sin vínculo (ve al solicitante) | Otro atleta / entrenador | Admin (sobre un gestionable) | Anónimo |
|---|---|---|---|---|---|---|---|
| `id` | L | L | L | – | – | L | – |
| `email` | L · C | – | – | – | – | L · C | C (registro) |
| `rol` | L · C | – | – | – | – | L · C | C (registro, sin ADMIN) |
| `passwordHash` | – | – | – | – | – | – | – |
| `nombre` | L · E | L | L | L | – | L · E | C |
| `apellido` | L · E | L | L | L | – | L · E | C |
| `fechaNacimiento` | L · E | – | – | – | – | L · E | C |
| `edad` (calculada) | L | L | L | L | – | L | – |
| `descripcion` | L · E | L | L | – | – | L · E | C |
| `pesoKg` (solo atleta) | L · E | L | n/a | – | – | L · E si es atleta | C si es atleta |
| `aniosServicio` (solo entrenador) | L · E | n/a | L | n/a | – | L · E si es entrenador | C si es entrenador |
| `creadoEn` | – | – | – | – | – | L | – |

El `passwordHash` se calcula con la contraseña del registro o del alta y nunca sale en ninguna
respuesta. No existe todavía un endpoint para cambiar la contraseña.

**`Rutina` y `EjercicioRutina`**

| Campo | Atleta dueño | Entrenador creador (vínculo vigente) | Entrenador sin vínculo / otro | Otro atleta | Admin |
|---|---|---|---|---|---|
| `id` | L | L | – | – | – |
| `nombre` | L · C | L · C (al asignarla) | – | – | – |
| `origen` (ASIGNADA / PROPIA) | L (lo fija el servidor) | L (lo fija el servidor) | – | – | – |
| `atleta` (dueño) | – (implícito) | – (implícito) | – | – | – |
| `entrenadorCreador` | L, solo nombre y apellido (`creadaPor`) | L, solo nombre y apellido | – | – | – |
| `creadaEn` | L | L | – | – | – |
| `ejercicios[]`: `id`, `orden`, `nombre`, `series`, `repeticiones` | L · C | L · C | – | – | – |

El entrenador ve solo las rutinas que **él asignó**; las rutinas propias del atleta no. Ninguna
rutina se puede editar hoy: cualquier campo del body fuera de `nombre` y `ejercicios` da 400.

**`Ejecucion` y `SerieEjecutada`**

| Campo | Atleta dueño | Entrenador creador de la rutina (vínculo vigente) | Otros | Admin |
|---|---|---|---|---|
| `id` | L | L | – | – |
| `fecha` | L · C | L | – | – |
| `rutina` (`idRutina`, `nombreRutina`) | L (sale del path al crear) | L | – | – |
| `atleta` | – (sale de la sesión) | – | – | – |
| `creadaEn` | – | – | – | – |
| serie: `idEjercicio`, `nombreEjercicio`, `nroSerie`, `cargaKg` | L · C | L | – | – |

**`Vinculo`**

| Campo | Atleta | Entrenador | Otros | Admin |
|---|---|---|---|---|
| `id` | – | – | – | – |
| `entrenador` / `atleta` | L (perfil público del entrenador) | L (perfil público del atleta) | – | – |
| `fechaInicio` (`desde`) | L | L | – | – |
| `fechaFin` | – | L (en `exatletas`) | – | – |

El vínculo no tiene body de escritura: se crea al aceptar una solicitud y se cierra al
desvincular, sin campos que el cliente mande.

**`SolicitudVinculacion`**

| Campo | Atleta emisor | Entrenador destinatario | Otros | Admin |
|---|---|---|---|---|
| `id` | L | L | – | – |
| `entrenador` | C (solo el `idEntrenador`); L perfil público | – (es él) | – | – |
| `atleta` | – (sale de la sesión) | L, solo nombre, apellido y edad | – | – |
| `estado` | L (cambia con `cancelar`) | L (cambia con `aceptar` / `rechazar`) | – | – |
| `creadaEn` | L | L | – | – |
| `resueltaEn` | – | – | – | – |

### Decisiones

1. **Qué ve el entrenador de su atleta (vínculo vigente):** nombre, apellido, **edad** (no la
   fecha de nacimiento), descripción y peso. **No ve el email.** Es lo que ya hacía
   `PerfilPublicoResponse.deAtleta`.
2. **Qué ve el entrenador de quien le pidió vínculo (todavía sin vínculo):** **solo nombre,
   apellido y edad.** Antes `SolicitudRecibidaResponse` devolvía el perfil completo del atleta
   (con `id`, `pesoKg` y `descripcion`) antes de que el entrenador aceptara; se corrigió. La
   pantalla de solicitudes solo mostraba nombre y edad, así que no cambia nada visible. El
   peso y la descripción aparecen cuando el vínculo está vigente.
3. **Qué ve el atleta del entrenador:** nombre, apellido, edad, descripción y años de
   servicio, tanto de su entrenador como de los entrenadores del listado para pedir vínculo.
   **No ve el email ni la fecha de nacimiento.**
4. **Del exatleta** el entrenador conserva solo nombre, apellido y las fechas del vínculo.
5. **El admin** ve la ficha completa de atletas y entrenadores, con el email, salvo el hash.
   Sobre sus propios datos edita por `PUT /api/perfil` con la misma lista blanca (sin peso ni
   años de servicio: su rol no los tiene).
6. **`email` y `rol` no se pueden cambiar después de creados, ni siquiera el admin.**

### Tests

`web/CamposPorRolTest` (20): compara el **conjunto exacto de claves** del JSON de cada
endpoint de lectura contra la matriz, y manda cada campo prohibido a cada endpoint de escritura.

| Caso | Test |
|---|---|
| `/api/auth/yo` y `/api/perfil` de atleta, entrenador y admin | `sesionPropia`, `perfilPropio` |
| El atleta ve del entrenador solo el perfil público (detalle, listado y solicitudes enviadas) | `atletaVeASuEntrenador`, `atletaVeEntrenadoresYSolicitudes` |
| El entrenador ve al atleta vigente con edad y peso | `entrenadorVeASuAtleta` |
| Antes del vínculo, el solicitante solo muestra nombre, apellido y edad | `entrenadorSinVinculoVeLoMinimo` |
| El exatleta solo muestra nombre y fechas | `entrenadorVeAlExatleta` |
| Rutinas y ejecuciones: mismas claves para el atleta y para el entrenador | `rutinas`, `ejecuciones` |
| El admin ve la ficha completa, sin hash | `adminVeLaFichaCompleta` |
| Ninguna respuesta a un tercero trae email, fecha de nacimiento ni hash | `ningunDatoPrivadoSeFiltra` |
| `PUT /api/perfil` rechaza `rol`, `email`, `id`, `passwordHash`, `creadoEn`, `atleta`, `origen`, `entrenadorCreador` (atleta y entrenador) | `perfilPropioRechazaCamposProhibidos` |
| `PUT /api/admin/usuarios/{id}` rechaza los mismos campos | `adminRechazaCamposProhibidos` |
| Registro público y alta por el admin rechazan campos que el cliente no decide | `altasRechazanCamposProhibidos` |
| El admin no puede poner `pesoKg` a un entrenador ni `aniosServicio` a un atleta, ni dejar al atleta sin peso | `adminNoMezclaCamposDeRol` |
| El alta por el admin tampoco mezcla campos de rol | `altaDelAdminNoMezclaCamposDeRol` |
| El admin editando su propio perfil no puede agregarse peso ni años | `adminNoSeAgregaCamposDeRol` |
| Rutinas (atleta y entrenador), ejecuciones y solicitudes rechazan campos prohibidos, también dentro de cada ejercicio y serie | `rutinasRechazanCamposProhibidos`, `ejecucionesRechazanCamposProhibidos`, `solicitudesRechazanCamposProhibidos` |

Cada test de escritura verifica que **la base no cambió** (usuarios con su hash, rutinas,
ejecuciones, solicitudes y vínculos) y que el mismo cuerpo sin el campo extra sí es válido, para
que el 400 no sea por otra causa. Complementan a `web/RegistroTest` y `web/PerfilTest`
(RS15). Se comprobó que los tests detectan una regresión: con un campo extra en el DTO público,
con el DTO anterior de la solicitud y sin la validación por rol del admin, fallan 6 de los 20.

**Verificación manual**

- Como `lucia.ferreyra@sherpa.dev`, abrir el detalle de un atleta y mirar en DevTools la
  respuesta de `/api/entrenador/atletas/{id}`: solo `id`, `nombre`, `apellido`, `edad`,
  `descripcion` y `pesoKg`.
- `curl` con un `PUT /api/perfil` que incluya `"rol":"ADMIN"` → 400.

---

## RS08 — Control de acceso basado en estado

Se aplica la definición de la sección de requerimientos del Informe: las operaciones dependen
del **estado del recurso**, no solo del rol y la relación. El servidor lo verifica antes de
modificar nada y responde 409 (o 404 si el recurso ya no existe para el usuario).

| Recurso | Regla de estado | Respuesta | Dónde |
|---|---|---|---|
| Solicitud de vinculación | Solo una solicitud `PENDIENTE` se puede cancelar, aceptar o rechazar | 409 | `ServicioVinculacion` |
| Solicitud de vinculación | Al aceptar una, las demás pendientes del atleta pasan a `CANCELADA` | — | `ServicioVinculacion.aceptarSolicitud` |
| Rutina asignada | Una rutina con ejecuciones registradas no se puede editar | 409 | `ServicioRutinas.actualizarAsignada` |
| Rutina asignada | Una rutina archivada (dada de baja) no se puede editar ni ejecutar | 404 | `ServicioRutinas`, consultas de `RutinaRepository` (`archivadaEn is null`) |

Tests: `RutinasTest.noEditaRutinaConEjecuciones`, `RutinasTest.rutinaArchivadaNoSeEditaNiEjecuta`,
`VinculacionTest.laOtraSolicitudCanceladaNoSePuedeAceptar`. En el código cada chequeo está
marcado con `// RS08`.

El control por rol en el servidor que el Informe agrupa con RS08 en la amenaza A2 sigue
cubierto por RS01, RS03 y RS05 (`PoliticaAcceso`).

RF4 conserva el registro final en un único POST; no introduce estados `EN_CURSO` /
`FINALIZADA` ni `@Version`, porque eso pertenece a la definición alternativa (numeración vieja)
de RS08 y contradice la definición aplicada. Las repeticiones siguen siendo las planificadas
en la rutina; RF4 registra la carga real por serie y no agrega repeticiones realizadas.

RF4 permite que el atleta elimine una ejecución propia o todo su histórico. El borrado total
afecta únicamente al atleta autenticado y exige confirmación explícita en la interfaz. La
eliminación queda registrada en auditoría sin incluir datos de las series.

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

Los eventos de denegación son `acceso_denegado`, `login_fallido` y `sesion_invalidada`. Se registran los 401
del entry point, los 403 del access denied handler y los 404 por recurso ajeno del handler
global. El `motivo` de un 403 es el que dio la política (`el endpoint exige el rol ATLETA`,
`request no contemplada`, `token CSRF invalido o ausente`), y las requests que rechaza el
firewall de Spring Security quedan como `request rechazada por el firewall` (RS02).

RF4 agrega `ejecucion_eliminada` (actor y UUID de ejecución) e
`historico_ejecuciones_eliminado` (actor solamente). Ambos son eventos de operación exitosa
y no incluyen cargas, repeticiones ni otros datos de la ejecución.

**Nunca se loguean** contraseñas, cookies, tokens, cabeceras ni cuerpos de request. Del
request solo salen el método y el path, sin query string. El email del admin tampoco se
registra al crearlo.

| Dónde | Qué hace |
|---|---|
| `security/AuditorAcceso.java` | `denegado(...)`, `loginFallido(...)`, `sesionInvalidada(...)`, `ejecucionEliminada(...)`, `historicoEjecucionesEliminado(...)` |
| `security/PuntoEntradaNoAutenticado.java` | 401 sin sesión |
| `security/ManejadorAccesoDenegado.java` | 403 fuera de rol, request no contemplada o CSRF inválido, con el motivo de la política |
| `config/SecurityConfig.java` | `requestRejectedHandler`: requests rechazadas por el firewall |
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

**Cómo está implementado.** La cadena de autorización termina en una regla que **siempre
deniega** (`DenegacionPorDefecto`, RS02), no en `authenticated()`. Todo lo que se sirve está
enumerado en `PoliticaAcceso` (ver "Política de autorización"):

- **Lista blanca pública**: `GET /api/csrf`, `POST /api/auth/login`, `POST /api/auth/registro`.
- **API por rol**: una regla por endpoint, con método y patrón exactos (RS01, RS02).
- **Estáticos**: solo los archivos del build del frontend, solo con `GET`.

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
| `security/PoliticaAcceso.java` | Rutas del SPA y estáticos, solo `GET` |
| `config/SecurityConfig.java` | `authorizeHttpRequests(...)` desde la política, terminando en `DenegacionPorDefecto` |
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

**269 tests** con JUnit 5, Spring Boot Test y MockMvc, concentrados en la lógica de negocio y
sobre todo en los controles de seguridad.

| Clase | Tests | Qué cubre |
|---|---|---|
| `web/VinculacionTest` | 29 | solicitudes, vínculos, IDOR, roles, RS10 |
| `web/DenegacionPorDefectoTest` | 11 | RS02: requests no contempladas, firewall, CORS |
| `security/CoberturaControlAccesoTest` | 9 | RS16: handlers ↔ política, matriz completa, `/error`, SPA |
| `security/PoliticaAccesoTest` | 5 | RS01, RS03, RS05: matriz por grupo de rol, auditoría, identidad |
| `security/IdentidadDesdeLaSesionTest` | 4 | RS16: identidad desde la sesión, controladores sin repositorios |
| `security/DefensaEnProfundidadTest` | 3 | RS01: segunda capa sin la regla de URL |
| `web/AdminTest` | 20 | panel de admin, no crear admins, RS10 al eliminar |
| `web/EjecucionesTest` | 22 | ejecuciones, validación, visibilidad y eliminación del historial |
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
`@BeforeEach` para que ningún test dependa del orden),
`support/CapturadorDeAuditoria.java` (appender en memoria sobre el logger `AUDITORIA`) y
`support/InspectorDeEndpoints.java` (lista los endpoints reales de todos los
`HandlerMapping`).

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

## RS16 — Toda petición pasa por el control de acceso

**Cómo está implementado.** RS01 y RS02 hacen que toda request pase por la política. RS16 lo
convierte en una garantía que **se verifica en cada build**: si alguien agrega un endpoint
sin regla, deja una regla sin endpoint o hace que un controlador reciba la identidad del
cliente, la suite falla con un mensaje que nombra método y path.

1. **Cobertura** (`security/CoberturaControlAccesoTest`). `support/InspectorDeEndpoints`
   recorre **todos** los `HandlerMapping` del contexto (controladores, recursos estáticos,
   welcome page de Spring Boot, `BasicErrorController`) y falla si aparece un tipo de mapping
   que no sabe leer. El test cruza esa lista con la política en los dos sentidos y después
   ejecuta la matriz completa regla × {sin sesión, ATLETA, ENTRENADOR, ADMIN}: 401 sin sesión,
   403 con un rol que no corresponde, ni 401 ni 403 con el que corresponde (400 o 404 por
   datos ficticios es válido).
2. **Identidad** (`security/IdentidadDesdeLaSesionTest`). Por reflexión sobre los handlers
   reales: ningún parámetro de path, query, cabecera, cookie ni campo del body (incluidos
   records anidados) representa al usuario actual (`rol`, `idUsuario`, `yo`, `esAdmin`, y
   `idAtleta` dentro de `/api/atleta/**` o `idEntrenador` dentro de `/api/entrenador/**`);
   ningún handler lee cabeceras ni cookies; ningún controlador accede a un repositorio.

Los tres comportamientos que tienen que hacer fallar el build están **demostrados** en la
propia suite con controladores que nunca se registran en la app (un `@GetMapping
("/api/atleta/debug")` sin regla, una regla huérfana, un controlador que recibe `idAtleta`,
lee una cabecera y usa un repositorio).

Probado también a mano: comentar la regla `DELETE /api/atleta/vinculo` y agregar un
`@GetMapping("/debug")` real en `AtletaRutinasController` hace fallar la suite con
`DELETE /api/atleta/vinculo: endpoint sin regla en PoliticaAcceso (...AtletaVinculacionController#desvincular...)`
y `GET /api/atleta/debug: endpoint sin regla en PoliticaAcceso (...)`.

**Caminos no obvios**

| Camino | Regla | Qué entrega |
|---|---|---|
| Forward del contenedor a `/error` | `* /error` → `DENEGADO`. La cadena de seguridad se aplica a todos los tipos de dispatch, incluido `ERROR` | 403 genérico, sin datos |
| Rutas del SPA (`ControladorSpa`) | `GET` explícito por patrón | el mismo `index.html` para cualquiera, sin datos de usuario |
| Estáticos (`/assets/**`, `/favicon.ico`) | `GET` explícito | solo archivos del build (RS13) |
| Welcome page de Spring Boot (raíz `/`) | `GET /`. Existe aunque `add-mappings` está apagado; la tapa `ControladorSpa` | el mismo `index.html` |
| Logout | lo atiende `LogoutFilter` antes de autorizar | 204, sin datos |

**Hallazgo corregido.** `AuthController` usaba `UsuarioRepository` directamente (respuesta
del login y `GET /api/auth/yo`). El id salía de la sesión, pero el controlador se salteaba
la capa de servicios. Lo detectó `losControladoresNoAccedenARepositorios`; ahora pasa por
`ServicioAutenticacion.usuarioDeLaSesion()`.

**Endpoints de RF2, RF4 y RF7.** El test no tiene una lista fija de endpoints: los `PUT` y
`DELETE` de rutinas (RF2), los `DELETE` de ejecuciones (RF4) y `PUT /api/perfil/preferencias`
(RF7) entraron a la verificación solos al integrarse, y cada uno necesitó su regla en
`PoliticaAcceso` para que la suite pase, que es exactamente la garantía.

**Decisión: reflexión, no ArchUnit.** La suite corre offline (`./mvnw -o`) y ArchUnit no está
en el repositorio local de Maven; además las reglas necesitan el patrón de cada endpoint,
que solo conoce el contexto de Spring. No se agregó ninguna dependencia. La regla opcional
"los repositorios de recursos de usuario no exponen `findById` pelado" no se implementó:
`JpaRepository` lo expone siempre y la convención 2 de RS04 se sigue sosteniendo por revisión.

| Dónde | Qué hace |
|---|---|
| `test/.../support/InspectorDeEndpoints.java` | Lista los endpoints reales de todos los `HandlerMapping` |
| `test/.../security/CoberturaControlAccesoTest.java` | Handlers ↔ política y matriz completa |
| `test/.../security/IdentidadDesdeLaSesionTest.java` | Identidad desde la sesión y capas |
| `service/ServicioAutenticacion.java` | `usuarioDeLaSesion()` (hallazgo) |

**Tests**

| Caso | Test |
|---|---|
| Se recorren todos los handlers, incluidos estáticos y `/error` | `CoberturaControlAccesoTest.seRecorrenTodosLosHandlers` |
| Todo endpoint tiene regla | `CoberturaControlAccesoTest.todoEndpointTieneRegla` |
| Ninguna regla huérfana | `CoberturaControlAccesoTest.ningunaReglaQuedaHuerfana` |
| Demostración: endpoint sin regla / regla huérfana / regla quitada | `unEndpointSinReglaSeDetecta`, `unaReglaHuerfanaSeDetecta`, `quitarUnaReglaSeDetecta` |
| Matriz completa regla × sesión | `CoberturaControlAccesoTest.matrizCompletaDeAcceso` |
| El forward a `/error` pasa por la política | `CoberturaControlAccesoTest.elForwardAErrorPasaPorLaPolitica` |
| El SPA no entrega datos | `CoberturaControlAccesoTest.lasRutasDelSpaNoEntreganDatos` |
| La identidad nunca viene del cliente | `IdentidadDesdeLaSesionTest.ningunDatoDelClienteEsLaIdentidad` |
| Ningún handler lee cabeceras ni cookies | `IdentidadDesdeLaSesionTest.ningunHandlerLeeCabecerasNiCookies` |
| Los controladores no acceden a repositorios | `IdentidadDesdeLaSesionTest.losControladoresNoAccedenARepositorios` |
| Demostración: controlador que confía en el cliente | `IdentidadDesdeLaSesionTest.unControladorQueRecibeLaIdentidadSeDetecta` |

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

## RF1 — Registro y autenticación de usuarios (SH-35)

Auditoría del flujo de registro y login contra el requerimiento. Es mayormente verificación:
no hizo falta tocar código de producción, solo se agregaron los tests que faltaban.

**Trazabilidad: qué pide RF1 → dónde se cumple → test**

| RF1 pide | Dónde se cumple | Test |
|---|---|---|
| Registrar eligiendo perfil (Entrenador o Atleta) | `AuthController.registrar`, `ServicioAutenticacion.registrar`, `RolRegistro` (sin ADMIN); `pages/publicas/Registro.tsx` | `web/RegistroTest.registraAtleta`, `registraEntrenador`, `noSePuedeRegistrarUnAdmin` |
| Tras registrarse se puede entrar con el perfil elegido | `POST /api/auth/login` + `GET /api/auth/yo` | `web/RegistroTest.registroYLoginComoAtleta`, `registroYLoginComoEntrenador` |
| Inicio de sesión seguro | BCrypt costo 12, sesión del lado del servidor, id de sesión regenerado, mensaje único de error | `web/LoginSesionTest` (15 tests), `service/PoliticaContrasenaTest` |
| Tablas de usuarios y credenciales | `domain/Usuario` (`passwordHash`, `rol`, `email` único) | `web/RegistroTest.laRespuestaNoFiltraElHash` |
| Pantallas de login y registro | `pages/publicas/Login.tsx`, `Registro.tsx`, `auth/SesionContext.tsx` | verificación manual (ver abajo) |
| Cierre de sesión | `POST /api/auth/logout` | `web/LoginSesionTest.elLogoutInvalidaLaSesion` |

**Conclusión de cada punto revisado**

| Punto | Conclusión | Detalle |
|---|---|---|
| Enumeración de usuarios en el registro | **Riesgo aceptado** | Un email repetido devuelve 409 "Ya existe una cuenta con ese email", así que el registro permite saber si un email tiene cuenta. El login no lo revela. Se acepta porque una respuesta neutra obligaría a avisar por correo "ya tenés cuenta", y el sistema no envía correos: el usuario se quedaría sin saber por qué no pudo registrarse. Es el trade-off habitual de UX contra enumeración. Test que fija el comportamiento: `RegistroTest.rechazaEmailDuplicado`. |
| Timing del login | **OK, medido** | Con BCrypt costo 12 (el de producción), 8 intentos con email existente y contraseña incorrecta dieron una mediana de **224 ms**; 8 con email inexistente, **224 ms** (rangos 222–228 y 221–225 ms). `DaoAuthenticationProvider` ya compara contra un hash ficticio cuando el usuario no existe, y `setHideUserNotFoundExceptions(true)` oculta la causa. No se agregó un test de tiempos porque serían inestables. |
| ¿El registro deja la sesión iniciada? | **OK, no la inicia** | `registrar` devuelve 201 sin cookie `SHERPASESSION` y sin sesión del lado del servidor; el frontend manda al login. Como no hay sesión, no hay nada que regenerar. Test: `RegistroTest.elRegistroNoIniciaSesion`. |
| Timeout absoluto de sesión | **Riesgo aceptado** | Solo hay timeout por inactividad (`server.servlet.session.timeout: 30m`). Una sesión usada de forma continua no vence nunca. Se acepta: la cookie es `HttpOnly` y `SameSite=Lax`, y el Informe v1 no pide vigencia máxima. Si se quisiera, habría que guardar la hora de creación en la sesión y cortarla en `FiltroSesionVigente`. |
| Validación espejo en el frontend | **OK** | `Registro.tsx` hace `required` y `minLength={12}`; el resto lo valida el backend. Los mensajes de `PoliticaContrasena` llegan en `$.error`, `cliente.ts` los lee en `leerError` y `mensajeDe` los muestra en el `Aviso`. El HTML solo es una ayuda: la política se aplica en el servidor. |

**Fuera de alcance, riesgo aceptado.** No hay rate limiting ni bloqueo de cuenta, ni
recuperación de contraseña ni MFA: el Informe v1 los deja fuera. Un atacante puede probar
contraseñas sobre una cuenta sin límite de intentos; lo único que lo frena es el costo de
BCrypt (~0,2 s por intento) y la política de contraseñas que descarta las más comunes.

**Verificación manual**

1. Registrarse en `/registro` como atleta, y luego como entrenador: ambos terminan en `/login`.
2. Entrar, recargar la página: sigue logueado (el estado se restaura con `/api/auth/yo`).
3. Salir y volver a entrar.
4. Login con una contraseña incorrecta: mensaje genérico, y `logs/auditoria.log` tiene un
   evento `login_fallido` con el email y sin la contraseña.

---

## Base de seguridad general

**Regla.** Si algo falla mientras se decide o se aplica la autorización (excepción, base
caída, datos inconsistentes), el resultado es **denegar**: la request no continúa, no se
ejecuta la lógica de negocio y no se devuelven datos parciales. RS14 define el *formato* de
los errores; RS18 define su *efecto* sobre la autorización.

**Falla → respuesta → auditoría**

| # | Falla | Respuesta | Auditoría (`logs/auditoria.log`) | Dónde |
|---|---|---|---|---|
| 1 | `UsuarioRepository.findById` lanza una excepción en `FiltroSesionVigente` (base caída, SQLite bloqueado) | **503** `{"error":"El servicio no esta disponible en este momento"}`. La request se corta; no sigue con el principal viejo de la sesión | evento `fallo_cerrado`, estado 503, id del usuario, motivo `FiltroSesionVigente: DataAccessResourceFailureException` | `security/FiltroSesionVigente` |
| 2 | La política de acceso lanza una excepción al evaluar (incluye rutas públicas y requests sin sesión) | **403** `{"error":"No tiene permisos para realizar esta accion"}` | evento `acceso_denegado`, estado 403, motivo `error al evaluar la politica (IllegalStateException)` | `security/PoliticaAcceso.exigir` |
| 3 | Un servicio lanza una excepción | **500** `{"error":"Ocurrio un error inesperado"}`, sin cuerpo parcial | el detalle queda en el log de la aplicación (`LOG.error`); no es una denegación de autorización, así que no va a la auditoría | `error/ManejadorGlobalErrores.inesperado` |
| 4 | JSON ilegible, parámetro faltante o UUID mal formado | **400** `{"error":"La solicitud no es valida"}`; el servicio no se ejecuta | — (RS14) | `ManejadorGlobalErrores.entradaInvalida` |
| 5 | El cuerpo no pasa Bean Validation | **400** con el nombre de los campos, nunca el valor recibido | — (RS14) | `ManejadorGlobalErrores.validacion` |
| 6 | Una excepción escapa de todo handler (por ejemplo, la lanza un filtro) y el contenedor reenvía a `/error` | **403** `{"error":"No tiene permisos para realizar esta accion"}`, JSON siempre, sin HTML ni stack trace | evento `acceso_denegado`, estado 403, **recurso original** y motivo `error interno no manejado (HTTP 500), denegado al reenviar a /error` | `ManejadorAccesoDenegado`, `AuditorAcceso` |
| 7 | Request directa a `/error` | **403** igual que el punto 6 | `acceso_denegado` (regla `/error` DENEGADO) | `PoliticaAcceso` |

En todos los casos el mensaje de la excepción **no sale por HTTP ni entra en la auditoría**:
la auditoría guarda solo el nombre de la clase de la excepción (RS17). El detalle técnico
queda en el log de la aplicación.

**Cómo está implementado**

- **Filtro** (`FiltroSesionVigente`): el `try/catch` cubre solo la recarga del usuario. Ante
  cualquier `RuntimeException` limpia el contexto de seguridad, audita y responde 503. No
  cubre `chain.doFilter`: lo que falle más abajo no es una falla de este filtro.
- **Política** (`PoliticaAcceso.exigir`): envuelve a `evaluar`. Los `AccessDeniedException` y
  `AuthenticationException` son decisiones (403 y 401) y siguen su camino; cualquier otra
  excepción se convierte en `AccesoDenegadoPorPolitica`, o sea 403. Como lo usan los dos
  puntos de aplicación de RS01 (filtro por URL e interceptor del handler), el fail closed
  vale en los dos.

**Decisiones**

1. **Base no disponible en el filtro → 503, no 401.** La falla es del servidor, no del
   cliente: un 401 haría que el SPA (`api/cliente.ts`) limpiara el estado y mandara al login,
   y por eso la sesión **no se invalida**. Cuando la base vuelve, el mismo usuario sigue
   logueado (`FallaCerradaTest.laSesionSobreviveALaFallaDeLaBase`). Lo que sí se descarta es
   el contexto de seguridad de ese request.
2. **`/error` queda DENEGADO (RS13), no se habilita para el forward del contenedor.** Es lo
   que ya definía la política de SH-22. Medido con un servidor real: una excepción que
   escapa de todo handler **vuelve como 403**, no como 500 ni como respuesta vacía: el
   dispatch de tipo ERROR pasa por la regla `/error` de la política y se
   deniega. Es fail closed y no filtra nada, así que se acepta. **Costo conocido:** un error
   interno real se le muestra al cliente como "sin permisos". Se mitiga en la auditoría
   (punto 6 de la tabla: recurso original y motivo "error interno no manejado") y en el log
   de la aplicación, donde sí está el stack trace. Si se prefiriera devolver 500, habría que
   permitir solo el dispatch `ERROR` hacia `/error` y servirlo con un `ErrorController`
   propio (en una prueba previa a SH-22 funcionó; hoy habría que ajustar también el
   interceptor). Cambia la regla de SH-22 y queda fuera de este issue.
3. **Los errores que no son de autorización (500, 400) no se auditan como denegación.** La
   auditoría es de autorización (RS12); un 500 de negocio deja rastro en el log de la
   aplicación.

**Tests** — `security/FallaCerradaTest` (8), `security/FallaCerradaRealTest` (4, con Tomcat
embebido), `security/PoliticaAccesoFallaCerradaTest` (5)

| Caso | Test |
|---|---|
| Base caída en el filtro → 503 genérico, el servicio no se invoca, auditado sin el mensaje de la excepción | `FallaCerradaTest.baseCaidaEnElFiltroCorta` |
| La misma falla corta también una escritura | `FallaCerradaTest.baseCaidaCortaTambienLasEscrituras` |
| La sesión sobrevive a una falla transitoria | `FallaCerradaTest.laSesionSobreviveALaFallaDeLaBase` |
| La política lanza → 403, el servicio no se invoca, auditado | `FallaCerradaTest.laPoliticaQueFallaDenega` |
| La política lanza → también se deniega una ruta pública sin sesión | `FallaCerradaTest.laPoliticaQueFallaDenegaHastaLoPublico` |
| Un servicio lanza → 500 con el cuerpo genérico exacto | `FallaCerradaTest.elServicioQueFallaDa500Generico` |
| JSON ilegible y validación fallida → 400, el servicio no se invoca | `FallaCerradaTest.jsonIlegibleDa400SinEjecutarNada`, `validacionFallidaDa400SinEjecutarNada` |
| Excepción de un filtro → 403 JSON, sin stack trace ni HTML, con `Accept` json, html y `*/*` | `FallaCerradaRealTest.laExcepcionNoManejadaDaUnaRespuestaGenerica`, `laCausaNoSale` |
| Esa excepción queda auditada con el recurso original | `FallaCerradaRealTest.elErrorNoManejadoSeAudita` |
| `/error` pedido directamente → 403 | `FallaCerradaRealTest.errorDirectoEstaDenegado` |
| La política no convierte en falla un 401/403 normal ni consulta la sesión en rutas públicas | `PoliticaAccesoFallaCerradaTest` |

Los tests del filtro y de la política se verificaron quitando el `try/catch` a propósito: sin
el fail closed fallan 6 de ellos (5 errores y 1 fallo).

**Verificación manual.**

- Con la app levantada, poner `data/sherpa.db` en solo lectura o borrarla **no** hace fallar
  el filtro: una base abierta sigue pudiendo leerse. Lo que cambia es la escritura: probar un
  `POST` autenticado (por ejemplo crear una rutina) y comprobar que devuelve el 500 genérico
  y ningún dato. El 503 del filtro está cubierto solo por el test automático.
- Con `curl -i http://localhost:8080/error` debe dar 403 con el cuerpo genérico.

---

## Base de seguridad general

### Mass assignment

Ver **RS06**: la defensa en capas contra mass assignment y la matriz de campos están ahí.

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
`localStorage` (lo único que se guarda ahí es el tema de RF7, para evitar el parpadeo inicial;
el valor que manda es el del servidor): la sesión vive en el servidor, así que cualquier cambio de autorización se
refleja en el request siguiente. Ante un 401 limpia el estado local y manda al login.
