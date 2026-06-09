# Silvalde Web

Tienda online + panel admin para un negocio de hogar. Monorepo con dos stacks separados.

## Stack y entrypoints

- `backend/`: API Spring Boot 4.0.6, Java 21, Maven (`./mvnw`), JPA + Security + Validation, JJWT 0.12.5, Lombok, MySQL 8 (prod), H2 (tests).
  - Entry: `backend/src/main/java/com/silvaldeweb/BackendApplication.java` (lleva `@EnableJpaAuditing`, no quitar).
- `frontend/`: React 19 + Vite 8 + JavaScript (no TypeScript) + Tailwind v4 (`@tailwindcss/vite`).
  - Entry: `frontend/src/main.jsx` → `App.jsx` (actualmente placeholder vacío, sin router ni páginas).
- `docker-compose.yml` (raíz): orquesta los tres servicios.
- `backend/docker-compose.yml`: alternativa solo con `mysql` + `backend` (lee `.env` de la raíz). Útil para desarrollar el backend con Docker sin tocar el frontend.

## Comandos

Asume `.env` en la raíz (ver "Setup" abajo).

| Tarea | Comando |
|---|---|
| Stack completo (recomendado) | `docker compose up --build` desde la raíz |
| Solo backend + MySQL vía Docker | `cd backend && docker compose up --build` |
| Backend local (sin Docker) | `cd backend && ./mvnw spring-boot:run` (necesita Java 21 y MySQL accesible) |
| Tests backend (H2 en memoria, sin MySQL) | `cd backend && ./mvnw test` |
| Test concreto backend | `cd backend && ./mvnw test -Dtest=ProductServiceTest` |
| Frontend dev (Vite, HMR) | `cd frontend && npm install && npm run dev` (puerto 5173) |
| Lint frontend | `cd frontend && npm run lint` |
| Build frontend | `cd frontend && npm run build` |
| Frontend no tiene `npm test` aún | — |

No hay CI ni pre-commit hooks configurados.

## Setup

1. `cp .env.example .env` (el `.env` real está en `.gitignore`; no commitearlo).
2. Reemplazar los placeholders (`change_me_*`, `silweb*`) por valores reales antes del primer `docker compose up`.
3. Backend en local sin Docker: exportar las mismas variables o usar los defaults de `backend/src/main/resources/application.properties`.

## Gotchas que un agente no adivinaría

- **Hostname de MySQL dentro de Docker = `mysql`, no `localhost`.** Si en local pruebas el backend sin Docker, usa `localhost:3306`. La extensión Spring Boot de VS Code auto‑lee `.env` al arrancar el main, así que si tienes `SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/...` en `.env`, el run local revienta con `UnknownHostException: mysql` (las env vars pisan a `application-{profile}.properties`).
- **Config por perfil, secretos por env var.** En este repo la URL de MySQL y CORS viven en `application-local.properties` (local) y `application-prod.properties` (Docker), NO en `.env`. En `.env` solo van secretos (passwords, JWT secret) y el nombre de perfil (`SPRING_PROFILES_ACTIVE=local`). El motivo: en Spring Boot las OS env vars tienen precedencia sobre los properties files, así que cualquier no-secreto en `.env` ganaría siempre y rompería el otro entorno.
- **Perfil Spring en Docker = `prod`** (`SPRING_PROFILES_ACTIVE: prod` en el compose raíz). No hay perfil `dev` definido.
- **Frontend en Docker NO habla CORS con el backend.** Nginx (`frontend/nginx.conf`) hace proxy de `/api/*` → `http://backend:8080`, mismo origen para el navegador. Usar rutas relativas `/api/...` en código de frontend.
- **Frontend en dev (`npm run dev`) SÍ habla CORS.** Llama al backend en `http://localhost:8080`. CORS está abierto para `http://localhost:5173` por defecto (`SecurityConfig.corsConfigurationSource`). Si cambias el puerto de Vite, ajusta `app.cors.allowed-origin` o añade el origen.
- **Java 21 obligatorio.** Configurado en `.vscode/settings.json` apuntando a `C:\Program Files\Java\jdk-21`. El `pom.xml` usa `spring-boot-starter-parent 4.0.6` con `java.version=21`.
- **`@EnableJpaAuditing` es necesario.** Vive en `BackendApplication`. Si lo quitas, los `@CreatedDate`/`@LastModifiedDate` de `Product` y `Category` dejan de poblarse en silencio.
- **Tests usan H2 en modo MySQL** (`MODE=MySQL`, `ddl-auto=create-drop`), no MySQL real. No hace falta MySQL levantado para `mvnw test`.

## Convenciones del repo

### Backend (paquetes por feature)
Cada feature nueva (p. ej. `order`, `address`) replica esta estructura bajo `com.silvaldeweb`:

```
<feature>/
  controller/<Feature>Controller.java
  service/<Feature>Service.java
  model/<Feature>.java            (entity JPA)
  repository/<Feature>Repository.java
  dto/<Feature>Request.java | <Feature>Response.java | <Feature>CreateRequest.java | <Feature>UpdateRequest.java
  exception/<Feature>NotFoundException.java | <Feature>AlreadyExistsException.java
```

- Las excepciones se registran en `exception/GlobalExceptionHandler.java` (devuelve `ProblemDetail` RFC 7807 con `path` y `timestamp`). Añadir handler ahí al crear excepciones nuevas.
- DTOs son `record` inmutables.
- **Lombok en entities**: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` (explícitos, NO `@Data`). `@ToString` solo si hace falta, marcando con `@ToString.Exclude` los campos sensibles (passwords) y las relaciones lazy.
- **Lombok en controllers/services**: solo `@RequiredArgsConstructor`.
- **Por qué NO `@Data` en entities JPA**: `@Data` añade `@EqualsAndHashCode` sobre todos los campos, lo que rompe con Hibernate (entities con mismo contenido pero distinto id se consideran iguales; proxies lazy hacen fallar `equals()`). También mete `@ToString` y `@RequiredArgsConstructor` que entran en conflicto con JPA. Si en un futuro una clase NO es entity JPA y quieres `@Data` (POJO plano, value object), úsalo con normalidad.
- **`@OneToMany`/`@OneToOne` con `cascade = ALL` + `orphanRemoval = true`**: la entidad padre (`Order`) es responsable del ciclo de vida de los hijos (`OrderItem`/`Payment`/`Shipment`). El padre no tiene `mappedBy` reverso, JPA usa la columna `order_id` FK del hijo. Guardar el padre persiste los hijos. Borrar el padre los borra en cascada.
- **Helpers para obtener el usuario autenticado**: `config/AuthUtils.java` expone `currentUser(Authentication, UserRepository)` (resuelve el `User` por email), `isAdmin(Authentication)` (chequea `ROLE_ADMIN`) y `currentEmail(Authentication)`. Usar SIEMPRE esto en controllers autenticados. El `userId` nunca debe venir del body de la request en endpoints que ya tienen usuario autenticado (prevención de IDOR).
- Auth actual: **DB-backed**. `DbUserDetailsService` carga usuarios de la tabla `users` por email. `DataSeeder` siembra `admin@example.com` y `customer@example.com` en el primer arranque (lee credenciales de `.env`). `LoginRequest` usa el campo `email` (no `username`).

### Backend (logging — obligatorio)
Toda clase con lógica de negocio o de infraestructura lleva `org.slf4j.Logger`:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(NombreClase.class);
```

Convenciones:
- **Services**: `log.info` al entrar con parámetros relevantes, `log.info` al terminar con éxito, `log.warn` en reglas de negocio violadas (e.g. SKU duplicado), `log.error(..., exception)` con stack trace en fallos inesperados. `delete` siempre va con `try/catch` que re-lanza tras loguear el motivo.
- **Controllers**: `log.info` por cada request entrante (`POST /api/categories name='X'`). Cero lógica de negocio en el controller.
- **Filtros de seguridad** (`JwtAuthenticationFilter`): `log.info` cuando un token se valida (`JWT valid for DELETE /api/categories/1 as user='admin' authorities=[ROLE_ADMIN]`), `log.warn` en tokens inválidos.
- **Exception handler**: `log.error(..., exception)` con stack en `handleGenericException` (catch-all). El resto puede usar `log.debug` para no spamear.
- **Niveles**: INFO en operación normal, WARN en situación anómala esperada, ERROR en fallo. No loguear secretos (passwords, tokens, JWT secret).

### Backend (validaciones — obligatorio)
Toda Request DTO lleva anotaciones de Bean Validation. Errores devuelven 400 con `ProblemDetail` (manejado por `GlobalExceptionHandler.handleValidationException`).

Checklist al crear/editar un DTO:
- **Strings requeridos**: `@NotBlank` (no confundir con `@NotEmpty` ni `@NotNull`).
- **Longitud**: `@Size(min=..., max=...)` siempre que aplique.
- **Email**: `@Email` además de `@NotBlank` y `@Size(max=255)`.
- **Unique fields**: la columna de la entity lleva `unique=true`; el DTO no lo repite (lo gestiona la BD + la excepción `AlreadyExists`).
- **Números positivos**: `@Positive` (estricto) o `@PositiveOrZero` (admite 0). Para decimales usar `@DecimalMin(value="0.0")` con mensaje claro.
- **Stock/cantidades**: `@Min(0)` o `@Min(1)` según la regla de negocio.
- **Teléfonos / códigos**: `@Pattern(regexp="^[0-9]{9}$")` con mensaje descriptivo.
- **Campos opcionales**: nada de `@NotNull`. Los `Boolean active` y enums como `Role` suelen ser opcionales (la entity tiene defaults).
- **Mensajes**: siempre en español y describiendo el problema (e.g. `"El email debe tener un formato válido"`).

Convenciones de seguridad en la entity (no en el DTO):
- **Passwords**: `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` para que Jackson NUNCA serialize la entity con la password. `@ToString.Exclude` si la entity tiene `@ToString` (defense in depth).
- **Relaciones lazy / bidireccionales**: `@JsonIgnore` para evitar loops en serialización, `@ToString.Exclude` para evitar `LazyInitializationException` en `toString()`.
- **Enums**: `@Enumerated(EnumType.STRING)` (no `ORDINAL`) para que la BD sea legible y no se rompa al reordenar valores.

### Frontend
- JavaScript puro, sin TypeScript. `eslint.config.js` aplica `js.configs.recommended` + `react-hooks` + `react-refresh`.
- Tailwind v4 con sintaxis nueva: `@import "tailwindcss";` en `index.css`, plugin `@tailwindcss/vite` en `vite.config.js`. No usar `tailwind.config.js` (no existe).
- `public/icons.svg` existe pero `index.html` solo referencia `/favicon.svg`. Es sprite para uso futuro.

## Estilo de trabajo

- Cambios pequeños y verificables. Commit con mensaje claro (ver `git log --oneline`).
- Explicar decisiones a nivel junior: el usuario está aprendiendo.
- Evitar refactors amplios no pedidos.
- Antes de cambios grandes (nueva tabla, nuevo módulo, tocar `SecurityConfig`/`docker-compose`), confirmar dirección con el usuario.
- **Nunca** introducir secretos en el repo. Variables sensibles siempre vía `.env` (gitignored).
- Diseñar para crecimiento: la tienda sumará más features (clientes, pedidos, métricas, dashboard). No atar código a "que funcione ya"; preferir APIs y estructuras que escalen.

## Skills disponibles

Cargar con la herramienta `skill` cuando la tarea lo requiera. Viven en `.agents/skills/`:

- `java-spring-boot` — backend Spring Boot.
- `frontend-design` — UI/maquetación React.
- `accessibility` — auditorías WCAG 2.2.
- `seo` — SEO público.
- `security` — secretos, env vars, Docker seguro, hardening.
- `hyperframes` — generación de videos.

`skills-lock.json` solo refleja un subconjunto; los que están en `.agents/skills/` son los usables.

## Documentación

- `README.md` (raíz): visión general, arranque rápido con Docker, roadmap v1 (admin, clientes, métricas).
- `doc.md` (raíz): notas técnicas internas (estructura, env vars, Docker, security actual, etc.).
- `frontend/README.md`: boilerplate por defecto de Vite, ignorable.

**Regla:** al implementar una nueva funcionalidad, actualizar `doc.md` para reflejar los cambios técnicos (nuevos endpoints, modelos, configs). No exponer fallos de seguridad reales en el doc — describir la medida, no el exploit.
