# Silvalde Web — Notas técnicas

Referencia técnica interna. Para arrancar y usar el proyecto, ver `README.md` (setup y arranque) y `docs/POSTMAN.md` (pruebas de la API). Las convenciones de trabajo están en `AGENTS.md`.

## 1. Stack

- **Backend**: Java 21, Spring Boot 4.0.6, JPA, Spring Security, JJWT 0.12.5, springdoc 3.0.3, bucket4j 8.10.1, Lombok, MySQL 8 (prod) / H2 (tests).
- **Frontend**: React 19, Vite 8, JavaScript, Tailwind v4, Nginx (prod).
- **Infra**: Docker Compose, Nginx como reverse proxy.

## 2. Estructura del repositorio

```
silvaldeWeb/
├── backend/                  API Spring Boot + Dockerfile + .dockerignore
│   ├── src/main/java/com/silvaldeweb/
│   │   ├── BackendApplication.java     (entrypoint, @EnableJpaAuditing)
│   │   ├── config/                       Security, JWT, RateLimit, OpenAPI, DataSeeder
│   │   ├── controller/<feature>/         REST endpoints
│   │   ├── service/<feature>/            lógica de negocio
│   │   ├── service/dev/                  DevSeedService (datos de ejemplo)
│   │   ├── model/<feature>/              entidades JPA + enums
│   │   ├── repository/<feature>/         Spring Data
│   │   ├── dto/<feature>/                records inmutables
│   │   └── exception/
│   │       ├── BusinessException.java    base para todos los errores de negocio
│   │       ├── GlobalExceptionHandler.java  @RestControllerAdvice
│   │       └── <feature>/                NotFound, AlreadyExists, etc.
│   └── src/test/...
├── frontend/                 React 19 + Vite 8 + Tailwind v4 + Nginx
├── docs/POSTMAN.md           guía de pruebas de la API
├── docker-compose.yml        stack completo (mysql + backend + frontend)
├── .env / .env.example       secretos y perfil activo (.env gitignored)
├── README.md                 quickstart + estado + roadmap
├── doc.md                    este archivo
└── AGENTS.md                 convenciones del repo
```

## 3. Configuración

### 3.1 Profiles

| Profile | URL de MySQL | CORS | DevTools | Uso |
|---|---|---|---|---|
| `local` (default en `.env`) | `jdbc:mysql://localhost:3306/...` | `http://localhost:5173` | sí | dev con VS Code + `mvnw spring-boot:run` |
| `prod` | `jdbc:mysql://mysql:3306/...` (hostname = nombre del servicio Docker) | `http://localhost` | no | Docker Compose |

Activos via `application-{profile}.properties`. Las URLs y el CORS viven aquí, no en `.env`.

### 3.2 Variables de entorno (`.env`)

| Variable | Tipo | Notas |
|---|---|---|
| `MYSQL_*` | secreto | Inicialización del contenedor MySQL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | secreto | Conexión backend → MySQL |
| `JWT_SECRET` | secreto | **Obligatorio** ≥32 bytes. Backend NO arranca si es el default placeholder |
| `JWT_EXPIRATION_MINUTES` / `JWT_ISSUER` | config | Default 120 min, issuer `silvalde-web` |
| `APP_ADMIN_EMAIL` / `_PASSWORD` / `_ROLES` | secreto | Seed admin. Password ≥12 chars, no `change_me_*` |
| `APP_CUSTOMER_EMAIL` / `_PASSWORD` / `_ROLES` | secreto | Seed customer |
| `SPRING_PROFILES_ACTIVE` | config | `local` (dev) o `prod` (Docker) |
| `MYSQL_USE_SSL` / `MYSQL_REQUIRE_SSL` | config | Solo `prod`, default `false`. Poner `true` en producción real |

> El rol del seed customer es `USER` (no `CUSTOMER`) en el enum `Role`. La env var se llama `APP_CUSTOMER_*` por consistencia histórica.

## 4. Backend

### 4.1 Estructura por feature (paquete)

Cada feature replica el mismo layout. Para crear una nueva, copiar el árbol de `product/` o `order/`:

```
<feature>/
  controller/<Feature>Controller.java
  service/<Feature>Service.java
  model/<Feature>.java + enums
  repository/<Feature>Repository.java
  dto/<Feature>Request.java | <Feature>Response.java
  exception/<Feature>NotFoundException.java | <Feature>AlreadyExistsException.java
```

Convenciones:
- **Entities**: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` (explícitos, **nunca `@Data`**). `@ToString.Exclude` + `@JsonIgnore` en relaciones LAZY y campos sensibles. `@EntityListeners(AuditingEntityListener.class)` para `createdAt`/`updatedAt` (`@EnableJpaAuditing` está en `BackendApplication`).
- **DTOs**: `record` inmutables con Bean Validation (`@NotBlank`, `@Size`, `@Pattern`, `@Email`, `@Min`, etc.) en español.
- **Services**: solo `@RequiredArgsConstructor` (no `@Data` ni `@AllArgsConstructor`).
- **SLF4J Logger** en cada service, controller, filtro de seguridad, exception handler. INFO en entrada/salida, WARN en reglas violadas, ERROR con stack en fallos inesperados. No loguear secretos.

### 4.2 Manejo de errores

Jerarquía:
- `BusinessException` (abstracta) lleva `HttpStatus status()`, `String title()`, `Map<String,Object> getProperties()`. Cada excepción de dominio la extiende.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) tiene solo 4 handlers:
  - `MethodArgumentNotValidException` → 400 con `errors[]` (validación de DTOs).
  - `BusinessException` → status + title + detail del propio exception.
  - `AuthenticationException` → 401.
  - `AccessDeniedException` → 403.
  - `Exception` (catch-all) → 500 logueado con stack.

Respuesta: `ProblemDetail` (RFC 7807) con `title`, `detail`, `path`, `timestamp`. Para añadir una nueva excepción: clase que extienda `BusinessException`. Nada más.

### 4.3 Seguridad

**Matchers en `SecurityConfig`** (orden importa: el primero que matchea gana):

| Patrón | Rol |
|---|---|
| `POST /api/auth/login` | `permitAll` (rate-limited a 10 req/min/IP por bucket4j) |
| `/actuator/health/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `OPTIONS /**` | `permitAll` |
| `DELETE /api/orders/**` | ADMIN |
| `GET/PUT/PATCH/POST /api/orders/**` | USER o ADMIN (ownership check en service) |
| `/api/carts/**` | USER o ADMIN |
| `POST/PUT/PATCH/DELETE /api/users` | ADMIN |
| `POST/PUT/PATCH/DELETE /api/products/**` | ADMIN |
| `POST/PUT/PATCH/DELETE /api/categories/**` | ADMIN |
| `/api/audit-logs/**` | ADMIN |
| Resto (incluye `GET /api/users`, `GET /api/products`, etc.) | cualquier autenticado |

**IDOR prevention**: `userId`/`customerId` **nunca** vienen del body. Se derivan siempre del JWT vía `AuthUtils.currentUser(authentication, userRepository)`. Los DTOs no tienen esos campos.

**JWT** (`config/JwtService.java`):
- HS256 con `JWT_SECRET` ≥32 bytes (fail-fast si no).
- Claims: `sub` (email), `roles`, `iss`, `iat`, `exp`.
- `JwtAuthenticationFilter` extrae `Authorization: Bearer <token>`, valida, y setea la `Authentication` en el `SecurityContextHolder`. Log a nivel `DEBUG` por request.

**`RateLimitFilter`**: solo en `POST /api/auth/login`, 10 req/min/IP. Devuelve 429 con `Retry-After`.

**Auditoría**: `AuditLogService.record(actor, action, entityName, entityId, metadata, ipAddress)` se ejecuta dentro de `TransactionSynchronizationManager.registerSynchronization(...).afterCommit()` — solo persiste el log si la transacción externa commitea con éxito. Si el actor no es ADMIN, skip silencioso. `record()` es fire-and-forget: nunca lanza excepciones.

**Password hashing**: BCrypt. `User.lastLogin` se setea en cada login.

**`DataSeeder`** se ejecuta al arranque y falla rápido si las passwords de seed son el default placeholder `change_me_*` o son <12 caracteres.

### 4.4 Stock real (reglas)

- `OrderService.pay()` valida y descuenta `Product.stock` en la misma transacción.
- `OrderService.cancel()` revierte el stock automáticamente si el estado anterior era `PAID`.
- `Product.@Version` previene concurrencia (optimistic locking): dos requests simultáneos que intenten decrementar el mismo producto, uno recibe `OptimisticLockingFailureException`.

### 4.5 Performance / N+1

- `OrderRepository.findById`, `findByCustomerId`, `findByCustomerIdAndStatus`, `findByStatus` llevan `@EntityGraph(attributePaths = {"items", "payment", "shipment"})`. Una sola query carga el árbol completo.
- `Order.items` lleva `@BatchSize(50)`. Borrar 50 items de un pedido son 2 round-trips, no 51.
- `Product.stock` lleva `@Version` para evitar updates perdidos (ver 4.4).
- `cart_items (cart_id, product_id)` y `order_items (order_id, product_id)` tienen `UNIQUE` constraint (en `@Table(uniqueConstraints=...)`). El merge en `CartService.addItem` no es la única defensa.

## 5. Modelo de datos

10 tablas en MySQL 8. Naming: `snake_case` vía `@Table(name=...)` explícito (consistencia, sin depender de `SpringPhysicalNamingStrategy`).

### `users` (DB-backed `UserDetailsService`)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK |
| `email` | VARCHAR(255) | UNIQUE NOT NULL, case-insensitive via service (`findByEmailIgnoreCase`) |
| `password` | VARCHAR(100) | BCrypt hash. `@JsonProperty(WRITE_ONLY)` + `@ToString.Exclude` |
| `name` | VARCHAR(120) | |
| `phone` | VARCHAR(40) | nullable, validado `@Pattern(^[0-9]{9}$)` en DTO |
| `active` | BOOLEAN | NOT NULL DEFAULT TRUE |
| `role` | VARCHAR(20) | NOT NULL, enum `Role` (`ADMIN`/`USER`) vía `@Enumerated(STRING)` |
| `last_login` | TIMESTAMP | nullable, seteado por `AuthService.login` |
| `email_verified` | BOOLEAN | NOT NULL DEFAULT FALSE (futuro) |
| `created_at`, `updated_at` | TIMESTAMP | audit |

### `categories`
`id`, `name` UNIQUE, `description`, `active`, audit.

### `products`
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK |
| `name` | VARCHAR(160) | NOT NULL |
| `sku` | VARCHAR(60) | UNIQUE NOT NULL |
| `description` | VARCHAR(1000) | nullable |
| `price` | DECIMAL(12,2) | NOT NULL, **leído del server siempre** (no del body) |
| `stock` | INT | NOT NULL, optimistic locked vía `@Version` |
| `active` | BOOLEAN | NOT NULL DEFAULT TRUE |
| `version` | BIGINT | NOT NULL DEFAULT 0, `@Version` |
| `category_id` | BIGINT | FK → `categories.id` |
| audit | | |

### `addresses` (1:N con `users`, owner via JWT)
`id`, `full_name`, `street`, `city`, `province`, `postal_code` (validado `^[0-9]{5}$`), `country`, `is_default`, `user_id` FK, audit. El `user_id` se deriva del JWT, **no viene en el body**.

### `carts` (1:1 con `users`)
`id`, `customer_id` UNIQUE FK, `status` enum `CartStatus` (`ACTIVE`/`ABANDONED`/`CONVERTED`), audit.

### `cart_items` (N:1 con `carts`, cascade ALL + orphanRemoval)
`id`, `cart_id` FK, `product_id` (FK lógica), `quantity`, audit. **Sin `unitPrice` ni `lineTotal`**: se re-leen de `products` en cada `GET /api/carts` y al checkout (precio en vivo). UNIQUE `(cart_id, product_id)`.

### `orders`
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK |
| `order_number` | VARCHAR(40) | UNIQUE NOT NULL, formato `ORD-YYYYMMDDHHMMSS-NNNN` |
| `customer_id` | BIGINT | FK → `users.id` (LAZY) |
| `status` | VARCHAR(20) | enum `OrderStatus`: `PENDING` → `PAID` → `SHIPPED` → `DELIVERED` + `CANCELLED` |
| `total` | DECIMAL(19,2) | NOT NULL, **siempre recalculado** desde `SUM(order_items.line_total)` |
| `shipping_address` | VARCHAR(500) | NOT NULL, denormalizado (snapshot) |
| audit | | |

### `order_items` (N:1 con `orders`)
`id`, `order_id` FK, `product_id` (FK lógica), `quantity`, `unit_price` (snapshot del precio en el momento de compra), `line_total` (`unitPrice × quantity`), audit. UNIQUE `(order_id, product_id)`.

### `payments` (1:1 con `orders`, cascade ALL + orphanRemoval)
`id`, `order_id` UNIQUE FK, `method` enum `PaymentMethod`, `status` enum `PaymentStatus`, `amount`, `provider_reference` (WRITE_ONLY — no se devuelve en JSON), `paid_at`, audit.

### `shipments` (1:1 con `orders`, cascade ALL + orphanRemoval)
`id`, `order_id` UNIQUE FK, `carrier`, `tracking_number`, `status` enum `ShipmentStatus`, `shipped_at`, `delivered_at`, audit.

### `audit_logs` (append-only)
`id`, `actor_id` FK → `users.id`, `action` enum `Action`, `entity_name` (String, nombre de la entity), `entity_id` (nullable), `metadata` (VARCHAR 2000, contexto libre), `ip_address` (VARCHAR 64, nullable, futuro), `created_at`. **Sin `updated_at`** (inmutable). Índices en `actor_id`, `(entity_name, entity_id)`, `created_at`.

### Diagrama ER

```
                    ┌──────────┐
                    │  users   │
                    └─────┬────┘
              ┌───────────┼────────────┬──────────────┐
              │ (1:N)     │ (1:N)       │ (1:1)        │
              ▼           ▼             ▼              │
       ┌──────────┐  ┌──────────┐  ┌──────────┐        │
       │addresses │  │  orders  │  │  carts   │        │
       └──────────┘  └────┬─────┘  └────┬─────┘        │
                          │             │ (1:N)        │
                ┌─────────┼─────────┐   ▼              │
                │ (1:N)   │ (1:1)   │ (1:1)  ┌──────────┐
                ▼         ▼         ▼        │ cart_    │
         ┌──────────┐┌────────┐┌──────────┐  │ items    │
         │order_    ││payments││shipments │  └────┬─────┘
         │items     │└────────┘└──────────┘       │
         └────┬─────┘                             │
              │ (FK lógica, no @ManyToOne)        │
              ▼                                   ▼
       ┌──────────┐         ┌──────────┐   (FK lógica)
       │ products │◄────────│categories│
       └──────────┘  (N:1)  └──────────┘

                    ┌────────────┐
                    │ audit_logs │ ── actor_id ──► users
                    │ (append-   │
                    │  only)     │  entity_name + entity_id
                    └────────────┘      apuntan a CUALQUIER entity

       Leyenda: ─── FK JPA ─── FK lógica
```

`Order.customer_id`, `Address.user_id`, `Cart.customer_id`, `AuditLog.actor_id` son FKs JPA (`@ManyToOne`/`@OneToOne`). `OrderItem.product_id` y `CartItem.product_id` son FKs lógicas (Long) para mantener las entities ligeras.

## 6. API REST

Todas las rutas requieren `Authorization: Bearer <token>` excepto `POST /api/auth/login`. Respuestas de error son `ProblemDetail` JSON. **Swagger UI**: `http://localhost:8080/swagger-ui.html`.

### Auth
- `POST /api/auth/login` — body `{email, password}` → `{token, email, roles, tokenType}`. Rate-limited a 10 req/min/IP.

### Endpoints por recurso

| Recurso | Endpoints | Notas |
|---|---|---|
| `/api/categories` | CRUD básico + `?active=` | Solo ADMIN para POST/PUT/DELETE |
| `/api/products` | CRUD básico + `?active=&categoryId=` | Solo ADMIN para POST/PUT/DELETE |
| `/api/users` | CRUD básico + `?active=&role=` | Solo ADMIN para POST/PUT/DELETE |
| `/api/addresses` | CRUD + `?userId=` | USER solo ve/edita las suyas, ADMIN bypass. **`userId` no viene del body** |
| `/api/orders` | CRUD + state machine (`/pay`, `/ship`, `/deliver`, `/cancel`) | USER ve solo las suyas, ADMIN ve todas. `DELETE` solo ADMIN |
| `/api/carts` | `GET /`, `POST /items`, `PATCH /items/{id}`, `DELETE /items/{id}`, `DELETE /`, `POST /checkout` | Identificado por JWT, no `{cartId}` en URL |
| `/api/audit-logs` | Solo `GET /` paginado, con filtros | Solo ADMIN. **Append-only** — no hay POST/PUT/DELETE |
| `/api/dev/seed` | `POST /` | Solo `local` y `prod` (no `test`). Idempotente. Carga 4 categorías, 8 productos, 1 dirección de ejemplo |

### Order state machine

```
                ┌──────────┐
                │ PENDING  │ ◄──── create (POST /orders o /carts/checkout)
                └────┬─────┘
        pay / cancel  │    \ cancel
                     ▼     \────────►┌──────────┐
                ┌──────────┐         │ CANCELLED│ (terminal)
                │   PAID   │         └──────────┘
                └────┬─────┘
        ship / cancel  │
                     ▼
                ┌──────────┐
                │ SHIPPED  │
                └────┬─────┘
          deliver /cancel (bloqueado)
                     ▼
                ┌──────────┐
                │ DELIVERED│ (terminal)
                └──────────┘
```

- `pay` descuenta `Product.stock` y crea `Payment`.
- `ship` requiere `PAID`, crea `Shipment`.
- `deliver` requiere `SHIPPED`, marca `shipment.deliveredAt`.
- `cancel` permitido en `PENDING` (no-op) o `PAID` (revierte stock). Bloqueado en `SHIPPED`/`DELIVERED`/`CANCELLED` → 422.

### HTTP status codes

| Código | Cuándo |
|---|---|
| 200 | OK |
| 201 | Created (POST que crea recurso) |
| 204 | No Content (DELETE) |
| 400 | Validation error (DTO inválido) |
| 401 | No autenticado / token inválido |
| 403 | Autenticado pero sin permisos / IDOR |
| 404 | Recurso no existe |
| 409 | Conflicto (ya existe, stock insuficiente, etc.) |
| 422 | Operación no permitida en el estado actual (pagar un pedido ya pagado, etc.) — `UNPROCESSABLE_CONTENT` (Spring 7 canónico) |
| 429 | Rate limit excedido (`Retry-After` header) |
| 500 | Error inesperado (catch-all) |

## 7. Frontend (estado actual)

- React 19 + Vite 8 + JavaScript (no TypeScript) + Tailwind v4.
- Tailwind v4: `@import "tailwindcss";` en `index.css` + plugin `@tailwindcss/vite` en `vite.config.js`. **No** `tailwind.config.js`.
- ESLint: `js.configs.recommended` + `react-hooks` + `react-refresh`.
- Estado actual: scaffold Vite. `App.jsx` placeholder vacío. Sin router ni páginas todavía.
- Docker: multi-stage `node:22-alpine` (build) → `nginx:1.27-alpine` (sirve `dist/` + proxy `/api/*` → `http://backend:8080`).
- Dev local: `npm run dev` en `:5173` con CORS abierto.

## 8. Docker

### Stack (`docker-compose.yml` raíz)

- `mysql` (mysql:8.0): healthcheck `mysqladmin ping`, volumen `mysql_data`, puerto `3306`.
- `backend` (build de `./backend/Dockerfile`): puerto `8080`, espera a `mysql` healthy, tiene su propio healthcheck en `/actuator/health`.
- `frontend` (build de `./frontend/Dockerfile`): puerto `80`, depende de `backend` healthy.

### `backend/Dockerfile` (multi-stage con BuildKit)

```dockerfile
# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -DskipTests dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- BuildKit cache mounts en `/root/.m2` (rebuilds 5x más rápidos en iteraciones).
- `USER app` (no-root).
- Healthcheck con `actuator/health`.

## 9. Dev local

| Escenario | Comando |
|---|---|
| Stack completo | `docker compose up --build` |
| Solo backend + MySQL | `cd backend && docker compose up --build` |
| Backend local (live reload) | `cd backend && ./mvnw spring-boot:run` con `SPRING_PROFILES_ACTIVE=local` |
| Frontend local (HMR) | `cd frontend && npm run dev` (puerto 5173, CORS abierto) |
| Tests backend | `cd backend && ./mvnw test` (117 tests, ~10s) |
| Tests integration | `cd backend && ./mvnw test -Dgroups=integration` (incluye `ApplicationContextSmokeTest`) |
| Lint frontend | `cd frontend && npm run lint` |
| Seed BD con datos | `curl -X POST http://localhost:8080/api/dev/seed` |

## 10. Seguridad aplicada

- Secretos fuera del repo (en `.env`, gitignored).
- JWT HS256, secret ≥32 bytes con fail-fast en arranque.
- API stateless, CSRF off, CORS por profile.
- Contraseñas con BCrypt.
- Roles vía enum `Role` + authorities `ROLE_*` (Spring Security).
- Matchers de autorización por verbo HTTP + path (no se confía solo en `@PreAuthorize`).
- IDOR prevenido: `userId`/`customerId` derivados del JWT, **nunca del body**.
- Rate limiting en `/api/auth/login` con bucket4j (10 req/min/IP, 429 con `Retry-After`).
- Audit log append-only con persistencia post-commit (no phantom logs).
- Stock real con optimistic locking (`@Version` en `Product`).
- Validación de `unitPrice` en `OrderService.create` desde la BD (el cliente no puede fijar precio).
- `unitPrice` ya no se acepta en `OrderItemRequest` (es server-side).
- Spring Boot Actuator para healthchecks de Docker.
- Errores sin stack traces en cliente (`spring.web.error.include-stacktrace=never`).

## 11. Roadmap (v1)

**Hecho**: entidades + CRUD + stock + audit + auth + rate limit + Swagger + dev seed.

**Pendiente**:
- **Frontend completo**: React Router 7 + axios + Context (auth/cart). Pantallas: landing, login, catálogo, producto, cart, checkout, orders, admin.
- **Paginación** en `GET /api/products`, `/api/users`, `/api/categories`, `/api/orders`.
- **Stock decrement real en `pay()` con `@Lock(PESSIMISTIC_WRITE)`** como alternativa al optimistic locking para casos de mucha concurrencia.
- **Pagos reales**: integración Stripe/PayPal vía `RestClient`. Webhooks de pago y shipment.
- **Carts abandonados**: cron job que marque `CartStatus.ABANDONED` tras X días sin actividad.
- **Audit log**: rellenar `ip_address` con un `HandlerInterceptor` que capture el `HttpServletRequest`. Auditar también cambios en `User` y `Product` (no solo deletes).
- **Email verification**: implementar `User.emailVerified` con un endpoint público `GET /api/auth/verify?token=...` que active el flag.
- **TLS**: terminar TLS en Nginx (Let's Encrypt + certbot) y deshabilitar `MYSQL_USE_SSL=false` en prod.
- **CI**: GitHub Actions que corra `mvn test`, `docker compose build`, y un test e2e con Newman.
- **Migraciones**: introducir Flyway cuando `ddl-auto=update` se quede corto (cambios de schema no triviales).

## 12. Cómo se conecta la aplicación

```
Navegador (http://localhost)
        │ HTTP
        ▼
Frontend — Nginx (puerto 80)
   /api/*  →  proxy_pass http://backend:8080
        │ HTTP
        ▼
Backend — Spring Boot (puerto 8080)
   JwtAuthFilter → Controller → Service → JPA → MySQL
        │                ↑
        │         GlobalExceptionHandler
        ▼
   Actuator /actuator/health (healthcheck Docker)
        │
        ▼
MySQL 8 (puerto 3306) — silvalde_web_db
   10 tablas: users, addresses, categories, products,
   carts, cart_items, orders, order_items, payments, shipments,
   audit_logs
```

Flujo típico (`GET /api/orders` para un customer):
```
Browser → Nginx proxy → JwtAuthFilter valida token
→ OrderController.list(status=null, authentication)
→ AuthUtils.currentUser → User(id=2)
→ OrderService.list(userId=2, isAdmin=false, status=null)
→ OrderRepository.findByCustomerId(2L)  [single SQL via @EntityGraph]
→ mapea a OrderResponse (proyecta user.id, items, payment, shipment)
→ 200 OK + JSON
```

### Capas

| Capa | Paquete | Qué hace | Qué NO hace |
|---|---|---|---|
| Filtros | `config/` | Auth (JWT), CORS, rate limit, sesión stateless | Lógica de negocio |
| Controller | `controller/<feature>/` | Recibe HTTP, valida body, llama al service, loguea | Acceso a BD, reglas de negocio, mapeo entity → DTO |
| Service | `service/<feature>/` | Reglas de negocio, coordina repos, mapea entity → DTO, loguea | Conocer HTTP, devolver `ResponseEntity` |
| Repository | `repository/<feature>/` | Queries JPA | Lógica |
| Model | `model/<feature>/` | Entidades JPA con auditoría, enums | DTOs |
| DTO | `dto/<feature>/` | `record` inmutables con Bean Validation | Anotaciones JPA |
| Exception | `exception/` | Tipos de error + mapeo a `ProblemDetail` | Try/catch en controllers |
| Config | `config/` | Beans (Security, JWT, OpenAPI, RateLimit, AuthUtils, DataSeeder) | Lógica |
