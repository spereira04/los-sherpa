# Los Sherpa

Plataforma web que conecta entrenadores con atletas. Los entrenadores crean rutinas para sus
atletas; los atletas las ven, las completan y registran las cargas que levantaron.

Proyecto de la materia **Seguridad en el Desarrollo de Software**. Los controles de seguridad
están documentados en [SECURITY.md](SECURITY.md).

---

## Requisitos

| Herramienta | Versión | Nota |
|---|---|---|
| JDK | 21 | Temurin 21 o cualquier distribución de OpenJDK 21 |
| Node.js | 20 o superior | solo para el frontend |
| Maven | no hace falta instalarlo | el repo trae el wrapper (`backend/mvnw`) |

No se necesita Docker ni una base de datos externa: la persistencia es SQLite y el archivo se
crea solo en `data/sherpa.db`.

Para verificar las versiones:

```bash
java -version    # tiene que decir 21
node -v
```

Si tenés varias versiones de Java instaladas, apuntá `JAVA_HOME` a la 21 antes de cada
comando:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # macOS
```

---

## Levantar en desarrollo

Son dos procesos: el backend en el puerto 8080 y el dev server de Vite en el 5173. Vite
proxea `/api` al backend, así que el navegador ve un solo origen y la cookie de sesión viaja
sin tocar CORS.

**Terminal 1 — backend con datos de ejemplo:**

# Windows (Powershell)
```bash
cd backend
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SEED_PASSWORD="clave de ejemplo para desarrollo"
$env:ADMIN_EMAIL="admin@lossherpa.local"
$env:ADMIN_PASSWORD="montana nieve 1953 cumbre"
.\mvnw.cmd spring-boot:run
```

# Linux / MacOS
```bash
cd backend
export SPRING_PROFILES_ACTIVE=dev
export SEED_PASSWORD='clave de ejemplo para desarrollo'
export ADMIN_EMAIL='admin@lossherpa.local'
export ADMIN_PASSWORD='montana nieve 1953 cumbre'
./mvnw spring-boot:run
```

**Terminal 2 — frontend:**

```bash
cd frontend
npm install
npm run dev
```

Abrí <http://localhost:5173>.

### Usuarios de ejemplo (solo con el perfil `dev`)

El seed se carga únicamente con `SPRING_PROFILES_ACTIVE=dev` y solo si la base está vacía.
Todos los usuarios de ejemplo comparten la contraseña que pusiste en `SEED_PASSWORD`.

| Rol | Email | Para ver |
|---|---|---|
| Entrenador | `lucia.ferreyra@sherpa.dev` | dos atletas activos, rutinas asignadas y un historial con progresión de cargas |
| Entrenador | `marcos.quiroga@sherpa.dev` | un atleta activo, un exatleta en el historial y dos solicitudes pendientes |
| Entrenador | `sofia.benitez@sherpa.dev` | una solicitud pendiente, sin atletas |
| Atleta | `ana.suarez@sherpa.dev` | rutinas asignadas y propias, y varias ejecuciones |
| Atleta | `carla.rios@sherpa.dev` | sin entrenador, con dos solicitudes enviadas |
| Atleta | `elena.cabrera@sherpa.dev` | desvinculada: conserva la rutina que le asignaron |

Para empezar de cero, borrá la base: `rm -f data/sherpa.db`.

## Verificar los controles de seguridad en la app

Los 173 tests cubren esto, pero con el seed cargado también se puede comprobar a mano.

### RS04 — control de acceso a nivel de dato

Entrá como `lucia.ferreyra@sherpa.dev` → **Mis atletas** → **Ana**.

Vas a ver **una sola** rutina ("Fuerza de piernas") y **4** ejecuciones. Ana tiene además
"Cardio de los martes", que es una rutina **propia**, con una ejecución. El backend no las
devuelve nunca: la condición `origen = ASIGNADA` está dentro de la consulta, no en un `if`
posterior.

Pedir un atleta ajeno da 404, no 403, para no confirmar que el recurso existe:

```bash
curl -i http://localhost:8080/api/entrenador/atletas/<uuid-de-otro-atleta>
```

### RS10 — impacto inmediato de los cambios de autorización

Abrí el detalle de Ana como Lucía en una pestaña. En otra, entrá como Ana y desvinculate.
Refrescá la pestaña de Lucía: perdió el acceso en el request siguiente, sin re-login.

Lo mismo con el borrado: eliminá un usuario desde el panel de admin mientras tiene la sesión
abierta y su próximo request devuelve 401.

### RS13 — deny by default, incluidos los estáticos

Todos tienen que dar 403 o 404, nunca contenido ni un listado:

```bash
curl -i http://localhost:8080/actuator/health          # 403
curl -i http://localhost:8080/application.yml          # 403
curl -i http://localhost:8080/assets/                  # 404, sin listar el directorio
curl -i http://localhost:8080/api/no-existe            # 403
```

### RS12 y RS17 — logs de autorización

```bash
tail -f backend/logs/auditoria.log
```

Una línea JSON por acceso denegado, con timestamp, id de usuario, IP, método, recurso y
motivo. Sin contraseñas, cookies ni cuerpos de request. Probá loguearte con una contraseña
incorrecta: el evento registra el email pero nunca la contraseña.

### Política de contraseñas

Intentá registrarte con `entrenamiento2026!`. Se rechaza: tiene 18 caracteres, pero su raíz
sin dígitos ni símbolos está en la lista de contraseñas filtradas.

---

## Levantar en modo "producción" local

Spring Boot compila el frontend, lo empaqueta como estáticos y sirve todo desde un solo
puerto. Es un único comando y un único proceso.

```bash
cd backend
./mvnw -P prod clean package

export ADMIN_EMAIL='admin@lossherpa.local'
export ADMIN_PASSWORD='montana nieve 1953 cumbre'
java -jar target/backend-1.0.0.jar
```

Abrí <http://localhost:8080>.

El perfil `prod` de Maven descarga Node, corre `npm install` y `npm run build`, y copia
`frontend/dist` dentro del jar. En este modo **no hay seed ni usuarios de ejemplo**: las
únicas credenciales que existen son el admin de las variables de entorno y las cuentas que
se registren.

---

## Variables de entorno

Ninguna tiene valor por defecto en el código ni en los archivos de configuración.

| Variable | Default | Para qué sirve |
|---|---|---|
| `ADMIN_EMAIL` | — | Email del admin que se crea al arrancar. **Si falta, no se crea ningún admin.** |
| `ADMIN_PASSWORD` | — | Contraseña del admin. Tiene que cumplir la política; si no, **la app no arranca** y el mensaje dice qué corregir. No puede contener la parte local de `ADMIN_EMAIL`, ni `ADMIN_NOMBRE`, ni `ADMIN_APELLIDO`: por eso `admin@...` + `...para el admin` no sirve. |
| `ADMIN_NOMBRE` | `Administrador` | Nombre del admin inicial. |
| `ADMIN_APELLIDO` | `del Sistema` | Apellido del admin inicial. |
| `SPRING_PROFILES_ACTIVE` | `default` | `dev` activa el seed de ejemplo. |
| `SEED_PASSWORD` | — | Contraseña compartida de los usuarios de ejemplo. Obligatoria con el perfil `dev`. |
| `SHERPA_DB` | `../data/sherpa.db` | Ruta del archivo SQLite. Queda fuera de cualquier directorio servido. |
| `SERVER_PORT` | `8080` | Puerto del backend. |
| `COOKIE_SECURE` | `false` | Ponelo en `true` al servir por HTTPS. |
| `CORS_ORIGENES` | `http://localhost:5173` | Orígenes permitidos, separados por coma. |
| `LOG_DIR` | `logs` | Directorio de los logs. Queda fuera de cualquier directorio servido. |

Los dos admins se crean una sola vez: si el email ya existe, no se vuelve a crear nada.

---

## Correr los tests

```bash
cd backend
./mvnw test
```

Son **173 tests** entre unitarios y de integración. La mayoría verifica los controles de
seguridad: 401 sin sesión, 403 fuera de rol, IDOR entre atletas y entre entrenadores,
visibilidad de rutinas propias, efecto inmediato de la desvinculación y del borrado de
usuarios, mass assignment, CSRF, estáticos y formato de los logs. El detalle de qué test
cubre cada requisito está en [SECURITY.md](SECURITY.md).

Para correr una sola clase:

```bash
./mvnw test -Dtest=VinculacionTest
```

Los tests usan SQLite **en memoria**, así que no tocan `data/sherpa.db`.

> **Si la suite tarda muchísimo**: agregá `-o` (`./mvnw -o test`) para que Maven trabaje
> offline. En macOS, además, el escáner de malware del sistema (XProtect) y Spotlight pueden
> inspeccionar cada jar que la JVM carga y hacer que la misma suite pase de 30 segundos a 17
> minutos. El trabajo real de los tests es de unos 20 segundos; si te pasa, cerrá lo que
> tengas abierto o excluí `~/.m2` de Spotlight.

El frontend se verifica con el compilador de TypeScript:

```bash
cd frontend
npm run build      # tsc -b && vite build
```

---

## Política de contraseñas

Alineada con **ASVS v5**, que prioriza el largo por sobre las reglas de composición.

**Se exige:**

- Mínimo **12 caracteres**.
- Máximo **128 caracteres** (el techo evita un DoS al hashear).
- Que no esté en una lista de **~10.045 contraseñas filtradas conocidas**. La comparación no
  es solo por valor exacto: también se prueba la *raíz* de la contraseña, sacándole los
  dígitos y símbolos de los extremos, y se detecta la repetición de una misma palabra. Así
  `entrenamiento2026!` y `amoramoramoramor` quedan rechazadas.
- Que no contenga tu email, nombre ni apellido.
- Que no sea un mismo carácter repetido.

**NO se exige** (a propósito):

- Mayúsculas, minúsculas, dígitos ni símbolos. No hay reglas de composición arbitrarias.
- Cambio periódico de contraseña.

**Se permite:**

- Espacios y cualquier carácter Unicode, incluidos los emoji. Las frases largas son la forma
  recomendada de llegar al mínimo.
- La contraseña se normaliza a Unicode NFKC antes de hashearla, así que la misma frase
  tecleada de distinta forma sigue validando.

Las contraseñas se guardan con **BCrypt y factor de trabajo 12**, por encima del default de
Spring Security (10). Nunca se registran en los logs ni se devuelven en ninguna respuesta.

---

## Si algo falla

**Empezar con la base limpia** (vuelve a sembrar el seed):

```bash
rm -f data/sherpa.db
```

**Matar lo que haya quedado colgado:**

```bash
pkill -f spring-boot:run
pkill -f "backend-1.0.0.jar"
pkill -f vite
```

**Ver qué está corriendo:**

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:5173 -sTCP:LISTEN
```

**"This localhost page can't be found" en el 5173.** El dev server de Vite quedó viejo
(arrancó antes de que existiera `frontend/index.html`). Ctrl+C en su terminal y `npm run dev`
de nuevo.

**La app no arranca y se queja de `ADMIN_PASSWORD`.** La contraseña no cumple la política, y
es a propósito que eso corte el arranque en lugar de dejar un admin débil. No puede contener
la parte local de `ADMIN_EMAIL`, ni `ADMIN_NOMBRE`, ni `ADMIN_APELLIDO`: por eso
`admin@...` junto con `...para el admin` no sirve. El mensaje de error dice qué corregir.

**Maven falla con `Cannot access central ... in offline mode`.** Le pasaste `-o` a un comando
que necesita bajar algo (por ejemplo `spring-boot:run` la primera vez). Corrélo sin `-o`.

## Estructura del repositorio

```
LosSherpa/
├─ backend/     Spring Boot 3.3 + Java 21 (API y, en modo prod, los estáticos)
├─ frontend/    React + Vite + TypeScript
├─ data/        sherpa.db (fuera de cualquier directorio servido)
├─ logs/        app.log y auditoria.log (fuera de cualquier directorio servido)
├─ README.md
└─ SECURITY.md  mapeo de cada requisito de seguridad del sprint
```

---

## Fuera de alcance

No se implementan, por definición del sprint: facturación, subida de archivos, edición de
rutinas o de ejecuciones, feedback del entrenador, notificaciones, recuperación de
contraseña, rate limiting y Docker.
