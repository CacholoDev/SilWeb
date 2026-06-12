# Silvalde Web - Notas técnicas

Referencia técnica interna. Se actualiza con cada nueva funcionalidad (entidad, endpoint, config nueva). Para arrancar el proyecto ver `README.md`; para normas de trabajo ver `AGENTS.md`.

## 1. Resumen

Tienda online + panel de administración. Monorepo con dos stacks separados orquestados por Docker.

## 2. Stack

- **Backend**: Spring Boot 4.0.6, Java 21, Spring Data JPA, Spring Security, Spring Validation, JJWT 0.12.5, Lombok, MySQL 8 (prod) / H2 (tests).
- **Frontend**: React 19, Vite 8, JavaScript (no TS), Tailwind v4, Nginx (prod).
- **Infra**: Docker Compose, Nginx como reverse proxy.

## 3. Estructura del repositorio

- `backend/`: API Spring Boot + `Dockerfile` + `docker-compose.yml` (alternativa solo mysql + backend).
- `frontend/`: SPA React + `Dockerfile` + `nginx.conf`.
- `docker-compose.yml` (raíz): stack completo (mysql + backend + frontend).
- `.env`, `.env.example`: secretos y nombre de perfil. `.env` gitignored.
- `doc.md` (este archivo), `README.md`, `AGENTS.md`: documentación.

## 4. Configuración y entorno

### 4.1 Properties files

- `application.properties` (raíz de `resources/`): defaults sensatos para arrancar sin env vars. Cualquier valor de configuración se lee como `${VAR:default}`.
- `application-local.properties`: overrides cuando `SPRING_PROFILES_ACTIVE=local` (dev con VS Code). URL `localhost:3306`, CORS `http://localhost:5173`.
- `application-prod.properties`: overrides cuando `SPRING_PROFILES_ACTIVE=prod` (Docker). URL `mysql:3306`, CORS `http://localhost`.

### 4.2 Variables de entorno (`.env`)

Solo **secretos** y el nombre de perfil. Las URLs y orígenes CORS **NO** van aquí porque en Spring Boot las OS env vars pisan a los properties files, así que cualquier no-secreto en `.env` ganaría siempre y rompería el otro entorno.

| Variable | Tipo | Uso |
|---|---|---|
| `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_DATABASE` | secreto | Inicialización del contenedor MySQL |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | secreto | Conexión backend → MySQL |
| `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`, `JWT_ISSUER` | secreto | Firma y validación de tokens |
| `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`, `APP_ADMIN_ROLES` | secreto | Usuario admin bootstrap (in-memory) |
| `APP_CUSTOMER_USERNAME`, `APP_CUSTOMER_PASSWORD`, `APP_CUSTOMER_ROLES` | secreto | Usuario customer bootstrap (in-memory) |
| `SPRING_PROFILES_ACTIVE` | config | `local` (VS Code) o `prod` (Docker) |

> El rol del cliente es `USER` (no `CUSTOMER`) en el enum `Role`. La env var se llama `APP_CUSTOMER_*` por consistencia con la bootstrap actual pero el valor es `USER`. Pendiente migrar a usuarios en MySQL y renombrar.

### 4.3 Perfiles

- `prod` (Docker): `SPRING_PROFILES_ACTIVE: prod` hard-codeado en `docker-compose.yml` raíz. Gana sobre `.env`.
- `local` (VS Code): se setea en `.env` del desarrollador. Carga `application-local.properties` y activa DevTools.

## 5. Docker

### 5.1 `docker-compose.yml` (raíz)

Tres servicios en la red `silvaldeweb-network`:

- `mysql` (mysql:8.0): healthcheck con `mysqladmin ping`, volumen `mysql_data` para persistencia, puerto `3306:3306`.
- `backend` (build local de `./backend/Dockerfile`): expone `8080:8080`, espera a `mysql` healthy.
- `frontend` (build local de `./frontend/Dockerfile`): expone `80:80`, depende de `backend`.

Importante: dentro de la red Docker, el backend se conecta a MySQL por el **nombre del servicio** (`mysql:3306`), no `localhost`.

### 5.2 `backend/docker-compose.yml`

Alternativa con solo `mysql` + `backend`, lee `.env` de la raíz con `env_file`. Útil para iterar en backend sin tocar el frontend.

### 5.3 Dockerfiles

Multi-stage:

- **Backend**: `maven:3.9-eclipse-temurin-21` (build) → `eclipse-temurin:21-jre` (runtime). Produce `backend-0.0.1-SNAPSHOT.jar`.
- **Frontend**: `node:22-alpine` (build con `npm install` + `npm run build`) → `nginx:1.27-alpine` sirviendo `dist/`.

### 5.4 `frontend/nginx.conf`

Sirve los assets estáticos y hace proxy de `/api/*` → `http://backend:8080`. Esto evita CORS en el navegador (mismo origen) y permite rutas relativas `/api/...` en el código de frontend.

## 6. Backend

### 6.1 Estructura de paquetes (por feature)

```
com.silvaldeweb
├── BackendApplication.java       (entrypoint, @EnableJpaAuditing)
├── config/                        (Security, JWT)
├── controller/<feature>/          (REST endpoints)
├── service/<feature>/             (lógica de negocio)
├── model/<feature>/               (entidades JPA + enums)
├── repository/<feature>/          (Spring Data)
├── dto/<feature>/                 (records inmutables)
└── exception/
    ├── GlobalExceptionHandler.java
    └── <feature>/                 (NotFound, AlreadyExists, ...)
```

Cada feature replica el mismo layout. Para crear una nueva feature (p. ej. `order`, `address`), copiar la estructura de `category/` o `product/`.

### 6.2 Entidades JPA

Todas usan:
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`.
- `@EntityListeners(AuditingEntityListener.class)` para auditoría.
- `createdAt` y `updatedAt` de tipo `java.time.Instant` con `@CreatedDate` y `@LastModifiedDate`.
- Hibernate crea/actualiza el schema con `ddl-auto=update`.

### 6.3 Enums

`Role` (en `model/user/`): `ADMIN`, `USER`. Mapeado con `@Enumerated(EnumType.STRING)` para legibilidad en BD.

### 6.4 JPA Auditing

Habilitado en `BackendApplication` con `@EnableJpaAuditing`. Si se quita, los `createdAt`/`updatedAt` de todas las entidades quedan `null` en silencio.

### 6.5 Manejo de errores

`exception/GlobalExceptionHandler` (`@RestControllerAdvice`) captura:
- `MethodArgumentNotValidException` → 400 con campos fallidos.
- `AuthenticationException` → 401.
- `AccessDeniedException` → 403.
- `<Feature>NotFoundException` → 404.
- `<Feature>AlreadyExistsException` → 409.
- `Exception` (catch-all) → 500.

Todas las respuestas son `ProblemDetail` (RFC 7807) con `title`, `detail`, `path`, `timestamp`. Al añadir una excepción nueva, registrar su handler aquí.

### 6.6 Logging

Convención (ver AGENTS.md para detalle):
- SLF4J Logger en services, controllers, filtros de seguridad y exception handler.
- INFO al entrar/salir de operaciones, WARN en reglas de negocio violadas, ERROR con stack trace en fallos inesperados.
- No loguear secretos.

### 6.7 Seguridad (Spring Security + JWT)

- `config/SecurityConfig.java` define el `SecurityFilterChain`:
  - CSRF off, sesión `STATELESS`, CORS controlado.
  - `POST /api/auth/login` y `/actuator/health/**` permitidos sin auth.
  - Resto requiere JWT válido.
- `config/JwtService.java`: genera/parsea JWT con JJWT 0.12.5. Firma derivada de `JWT_SECRET` con SHA-256. Claims: `sub` (email), `roles`, `iss`, `iat`, `exp`.
- `config/JwtAuthenticationFilter.java`: filtro `OncePerRequestFilter` que extrae `Authorization: Bearer <token>`, valida y setea la `Authentication` en `SecurityContextHolder`.
- `config/DbUserDetailsService.java`: implementa `UserDetailsService` cargando de la tabla `users` por email (no más `InMemoryUserDetailsManager`).
- `config/DataSeeder.java`: `CommandLineRunner` que siembra `admin@example.com` (rol `ADMIN`) y `customer@example.com` (rol `USER`) en el primer arranque si no existen. Lee credenciales de `APP_ADMIN_*` / `APP_CUSTOMER_*` en `.env`.
- `AuthenticationManager` + `UserDetailsService`: ahora **DB-backed** (entidad `User`).
- `PasswordEncoder`: BCrypt.
- `CorsConfigurationSource`: orígenes y métodos permitidos parametrizados por `app.cors.allowed-origin`.

### 6.8 DevTools

`org.springframework.boot:spring-boot-devtools` con `<optional>true</optional>`. Excluido del fat jar por el plugin de Spring Boot (no llega a producción). Activa hot reload al ejecutar el backend en local.

## 7. Frontend

- React 19 + Vite 8 + JavaScript (no TypeScript) + Tailwind v4.
- Tailwind v4 con sintaxis nueva: `@import "tailwindcss";` en `index.css` y plugin `@tailwindcss/vite` en `vite.config.js`. No usar `tailwind.config.js` (no existe).
- ESLint con `js.configs.recommended` + `react-hooks` + `react-refresh`.
- `src/App.jsx` es placeholder; no hay router ni páginas todavía.
- `public/icons.svg` es sprite futuro; `index.html` solo referencia `/favicon.svg`.

## 8. Modelo de datos

### `categories`
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `name` | VARCHAR(120) | UNIQUE NOT NULL |
| `description` | VARCHAR(500) | nullable |
| `active` | BOOLEAN | NOT NULL DEFAULT TRUE |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### `products`
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `name` | VARCHAR(160) | NOT NULL |
| `sku` | VARCHAR(60) | UNIQUE NOT NULL |
| `description` | VARCHAR(1000) | nullable |
| `price` | DECIMAL(12,2) | NOT NULL |
| `stock` | INT | NOT NULL |
| `active` | BOOLEAN | NOT NULL DEFAULT TRUE |
| `category_id` | BIGINT | FK → `categories.id` NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### `users`
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `email` | VARCHAR(255) | UNIQUE NOT NULL |
| `password` | VARCHAR(100) | NOT NULL, BCrypt hash |
| `name` | VARCHAR(120) | NOT NULL |
| `phone` | VARCHAR(40) | nullable, validado con `@Pattern(^[0-9]{9}$)` en DTO |
| `active` | BOOLEAN | NOT NULL DEFAULT TRUE |
| `role` | VARCHAR(20) | NOT NULL, enum (`ADMIN` \| `USER`) |
| `last_login` | TIMESTAMP | nullable |
| `email_verified` | BOOLEAN | NOT NULL DEFAULT FALSE |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### `addresses`
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `full_name` | VARCHAR(120) | NOT NULL |
| `street` | VARCHAR(200) | NOT NULL |
| `city` | VARCHAR(100) | NOT NULL |
| `province` | VARCHAR(100) | NOT NULL |
| `postal_code` | VARCHAR(5) | NOT NULL, validado con `@Pattern(^[0-9]{5}$)` en DTO |
| `country` | VARCHAR(100) | NOT NULL |
| `is_default` | BOOLEAN | NOT NULL DEFAULT FALSE |
| `user_id` | BIGINT | FK → `users.id` NOT NULL, LAZY |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

> La relación `@ManyToOne Address → User` lleva `@ToString.Exclude` y `@JsonIgnore` (defense in depth: evita loops en serialización y `LazyInitializationException` en `toString()`).

### `orders`
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `order_number` | VARCHAR(40) | UNIQUE NOT NULL, formato `ORD-YYYYMMDDHHMMSS-NNNN` |
| `customer_id` | BIGINT | FK → `users.id` NOT NULL, LAZY |
| `status` | VARCHAR(20) | NOT NULL, enum `OrderStatus` |
| `total` | DECIMAL(19,2) | NOT NULL DEFAULT 0, **siempre recalculado** desde `SUM(order_items.line_total)` |
| `shipping_address` | VARCHAR(500) | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### `order_items`
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `order_id` | BIGINT | FK → `orders.id` NOT NULL, LAZY |
| `product_id` | BIGINT | FK lógica → `products.id` (sin `@ManyToOne` para mantenerlo ligero) |
| `quantity` | INT | NOT NULL, `>= 1` |
| `unit_price` | DECIMAL(19,2) | NOT NULL, snapshot del precio en el momento de compra |
| `line_total` | DECIMAL(19,2) | NOT NULL, `unitPrice * quantity` |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### `payments` (1:1 con `orders`)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `order_id` | BIGINT | FK UNIQUE NOT NULL, LAZY (cascade ALL) |
| `method` | VARCHAR(20) | NOT NULL, enum `PaymentMethod` |
| `status` | VARCHAR(20) | NOT NULL, enum `PaymentStatus` |
| `amount` | DECIMAL(19,2) | NOT NULL |
| `provider_reference` | VARCHAR(200) | nullable, `WRITE_ONLY` (no se devuelve en JSON) |
| `paid_at` | TIMESTAMP | nullable |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### `shipments` (1:1 con `orders`)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `order_id` | BIGINT | FK UNIQUE NOT NULL, LAZY (cascade ALL) |
| `carrier` | VARCHAR(80) | nullable (se rellena al marcar como enviado) |
| `tracking_number` | VARCHAR(120) | nullable |
| `status` | VARCHAR(20) | NOT NULL, enum `ShipmentStatus` |
| `shipped_at` | TIMESTAMP | nullable |
| `delivered_at` | TIMESTAMP | nullable |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

> Reglas de negocio clave: `Order.total` se calcula en el service y no acepta override (precio histórico fiable). `OrderItem.unitPrice` es snapshot (no se actualiza aunque cambie `Product.price`). Las transiciones de estado están controladas: `PENDING → PAID → SHIPPED → DELIVERED`, con `CANCELLED` como estado terminal (no se puede cancelar un pedido ya enviado o entregado).

### `carts` (1:1 con `users`)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `customer_id` | BIGINT | FK → `users.id` UNIQUE NOT NULL, LAZY (1 cart activo por user) |
| `status` | VARCHAR(20) | NOT NULL, enum `CartStatus` (`ACTIVE`, `ABANDONED`, `CONVERTED`) |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### `cart_items` (N:1 con `carts`, cascade ALL + orphanRemoval)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `cart_id` | BIGINT | FK → `carts.id` NOT NULL, LAZY |
| `product_id` | BIGINT | FK lógica → `products.id` (sin `@ManyToOne` para mantenerlo ligero) |
| `quantity` | INT | NOT NULL, `>= 1` |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

> Diferencia clave con `OrderItem`: `cart_items` **no** tiene `unitPrice` ni `lineTotal` persistidos. El precio se re-lee de `products` en cada `GET /api/carts` y al hacer checkout, para que el usuario siempre vea el precio real (no uno stale). En el checkout, `OrderItem.unitPrice` se congela como snapshot del precio actual de `Product` en ese instante.

### `audit_logs` (append-only, solo ADMIN)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT | PK auto |
| `actor_id` | BIGINT | FK → `users.id` NOT NULL, LAZY (debe ser ADMIN para loguear) |
| `action` | VARCHAR(20) | NOT NULL, enum `Action` (`CREATE`, `UPDATE`, `DELETE`, `STATE_CHANGE`, `LOGIN`, `LOGOUT`) |
| `entity_name` | VARCHAR(60) | NOT NULL, nombre de la entity afectada (`Order`, `Product`, etc.) |
| `entity_id` | BIGINT | nullable (acciones sin target, ej. `LOGIN`) |
| `metadata` | VARCHAR(2000) | nullable, contexto extra: "PAID -> SHIPPED carrier=SEUR tracking=..." o "shippingAddress: calle A -> calle B" |
| `ip_address` | VARCHAR(64) | nullable, IP del cliente (futuro) |
| `created_at` | TIMESTAMP | NOT NULL, **inmutable** (sin `updated_at`) |

> **Reglas de negocio clave**: `audit_logs` es **append-only** — sin UPDATE, sin DELETE, sin endpoint para modificar. Solo `GET /api/audit-logs` (admin). La entity lleva índices en `actor_id`, `(entity_name, entity_id)` y `created_at` para queries rápidas de "qué hizo el admin X con el order Y" o "todos los cambios de estado del order Z". El log se traga errores (`@Transactional(propagation = REQUIRES_NEW)` + try/catch) para que un fallo en la BD de audit NUNCA tumbe una operación de negocio. El `AuditLogService.record(...)` filtra internamente: si el actor es null o no es ADMIN, no se persiste nada.

## 9. API REST

Todas las rutas requieren `Authorization: Bearer <token>` excepto `POST /api/auth/login`. Respuestas de error son `ProblemDetail` JSON.

### Auth
- `POST /api/auth/login` — body `{email, password}` → `{token, email, roles, tokenType}`.

### Categories (`/api/categories`)
- `POST` crear · `GET` listar (`?active=`) · `GET /{id}` detalle · `PUT /{id}` actualizar · `DELETE /{id}` eliminar.

### Products (`/api/products`)
- `POST` crear · `GET` listar (`?active=&categoryId=`) · `GET /{id}` detalle · `PUT /{id}` actualizar · `DELETE /{id}` eliminar.

### Users (`/api/users`)
- `POST` crear (hashea password) · `GET` listar (`?active=&role=`) · `GET /{id}` detalle · `PUT /{id}` actualizar (password opcional) · `DELETE /{id}` eliminar.

### Addresses (`/api/addresses`)
- `POST` crear · `GET` listar (`?userId=`) · `GET /{id}` detalle · `PUT /{id}` actualizar · `DELETE /{id}` eliminar.

### Orders (`/api/orders`)
- `POST /api/orders` (USER/ADMIN) crear — body `{items: [{productId, quantity, unitPrice}], shippingAddress}`. `customerId` se toma del JWT, nunca del body.
- `GET /api/orders` (USER/ADMIN) listar — `?status=PENDING|PAID|SHIPPED|DELIVERED|CANCELLED`. USER ve solo sus pedidos; ADMIN ve todos.
- `GET /api/orders/{id}` (USER/ADMIN) detalle — USER solo si es owner; ADMIN siempre.
- `PUT /api/orders/{id}` (USER/ADMIN) actualizar `shippingAddress` — solo en `PENDING`.
- `PATCH /api/orders/{id}/pay` (USER/ADMIN) pagar — body `{method, providerReference}`. Transición `PENDING → PAID`. **Valida y descuenta stock real en la misma transacción** (`Product.stock -= OrderItem.quantity` por cada item). Si algún item no tiene stock suficiente → 409 con `productId`/`requested`/`available` en el body (`InsufficientStockException`). Solo owner o ADMIN.
- `PATCH /api/orders/{id}/ship` (USER/ADMIN) enviar — body `{carrier, trackingNumber}`. Transición `PAID → SHIPPED`. Solo owner o ADMIN.
- `PATCH /api/orders/{id}/deliver` (USER/ADMIN) marcar entregado — transita `SHIPPED → DELIVERED` y actualiza `shipment.deliveredAt`.
- `PATCH /api/orders/{id}/cancel` (USER/ADMIN) cancelar — solo si está en `PENDING` o `PAID`. Bloqueado en `SHIPPED`/`DELIVERED`/`CANCELLED`.
- `DELETE /api/orders/{id}` (**solo ADMIN**) eliminar.

> Reglas de autorización: el owner se obtiene SIEMPRE del JWT (`AuthUtils.currentUser` resuelve el `User` por email y compara `id` con `Order.customer.id`). Si un USER pide un pedido que no es suyo se lanza `AccessDeniedException` → 403. Los endpoints `/api/orders/**` aceptan tanto USER como ADMIN; `DELETE` es exclusivo de ADMIN vía matcher en `SecurityConfig`. Los recursos `OrderItem`/`Payment`/`Shipment` no tienen controllers propios, se acceden siempre dentro del payload de `Order`.

### Carts (`/api/carts`)
- `GET /api/carts` (USER/ADMIN) devuelve el cart activo del user autenticado, creándolo si no existe.
- `POST /api/carts/items` (USER/ADMIN) añade o mergea — body `{productId, quantity}`. Si el producto ya está en el cart, suma `quantity` en lugar de duplicar.
- `PATCH /api/carts/items/{itemId}` (USER/ADMIN) cambia la cantidad — body `{quantity}`. Borra el item si se hace desde otra ruta.
- `DELETE /api/carts/items/{itemId}` (USER/ADMIN) borra el item del cart.
- `DELETE /api/carts` (USER/ADMIN) vacía el cart (los items se eliminan, el cart persiste con `status=ACTIVE`).
- `POST /api/carts/checkout` (USER/ADMIN) convierte el cart en un `Order` en estado `PENDING` — body `{shippingAddress}`. Valida stock de todos los items antes de crear el pedido. Devuelve el `OrderResponse` directamente. Tras éxito, vacía el cart y lo marca `status=CONVERTED`.

> Reglas de autorización: el cart SIEMPRE se identifica por el usuario autenticado. No se expone `{cartId}` en URL — `GET /api/carts` resuelve internamente `findByCustomerId(jwt.userId)` o crea uno nuevo. Imposible acceder al cart de otro user. El campo `unitPrice` en los items del `CartResponse` se lee en vivo de `products` (puede cambiar entre requests). En checkout, si algún item no tiene stock suficiente (`product.stock < quantity`), se lanza `InsufficientStockException` → 409 con `productId`, `requested`, `available` en el body.

### Audit Logs (`/api/audit-logs`, **solo ADMIN**)
- `GET /api/audit-logs` — filtros opcionales: `?actorId=`, `?entity=Order`, `?entityId=10`, `?action=DELETE`, `?from=2026-01-01T00:00:00Z`, `?to=2026-12-31T23:59:59Z`. Paginación: `?page=0&size=20` (default 20, orden `createdAt DESC`).
- **No hay POST/PUT/DELETE**: el log es append-only, no se puede modificar vía API.

> Los logs los emite el `AuditLogService.record(actor, action, entityName, entityId, metadata, ipAddress)`, llamado desde los services críticos (`OrderService.create/pay/ship/deliver/cancel/delete/update`, y el `AuthService` en login/logout cuando se implemente). El método es fire-and-forget: nunca lanza excepciones, nunca hace fallar la operación de negocio, y se persiste en una transacción independiente (`REQUIRES_NEW`) para que un rollback del log no afecte al commit de la operación. Si el actor no es ADMIN, el record no persiste nada (skip silencioso, logueado a nivel `DEBUG`).

**Ejemplo de query útil** (qué pasó con el order 50):

```bash
GET /api/audit-logs?entity=Order&entityId=50
```

Respuesta:
```json
{
  "content": [
    { "id": 1, "actorEmail": "customer@example.com", "action": "CREATE",  "metadata": "orderNumber=ORD-... total=124.48 items=2", "createdAt": "..." },
    { "id": 2, "actorEmail": "customer@example.com", "action": "STATE_CHANGE", "metadata": "PENDING -> PAID via CARD", "createdAt": "..." },
    { "id": 3, "actorEmail": "admin@example.com",     "action": "STATE_CHANGE", "metadata": "PAID -> SHIPPED carrier=SEUR tracking=TRACK-1", "createdAt": "..." }
  ],
  "pageable": {...},
  "totalElements": 3
}
```

## 10. Dev local

| Escenario | Comando | Notas |
|---|---|---|
| Stack completo | `docker compose up --build` (raíz) | MySQL, backend y frontend en Docker. |
| Solo backend + MySQL | `cd backend && docker compose up --build` | Frontend se gestiona aparte. |
| Backend local (live reload) | `cd backend && ./mvnw spring-boot:run` con `SPRING_PROFILES_ACTIVE=local` en `.env` | DevTools reinicia al cambiar clases. |
| Frontend local (HMR) | `cd frontend && npm run dev` | Puerto 5173, CORS ya abierto. |
| Tests backend | `cd backend && ./mvnw test` | H2 en memoria, no necesita MySQL. |
| Lint frontend | `cd frontend && npm run lint` | |

## 11. Seguridad aplicada

- Secretos fuera del repo (en `.env`, gitignored).
- JWT firmado (HS256 sobre `JWT_SECRET` derivado con SHA-256).
- API stateless, CSRF off, CORS por perfil.
- Contraseñas con BCrypt.
- Roles vía enum `Role` (en entidades) y authorities `ROLE_*` (en JWT).
- Errores sin filtrar stack traces al cliente (`spring.web.error.include-stacktrace=never`).
- **Autorización por rol en `SecurityConfig`**: los endpoints de mutación (`POST`/`PUT`/`PATCH`/`DELETE`) en `/api/users`, `/api/products`, `/api/categories` son **solo ADMIN** (matchers en `SecurityConfig.authorizeHttpRequests`). Los GET admiten USER y ADMIN. Los endpoints de `/api/orders` admiten USER (solo sobre sus propios orders, ownership check en `OrderService.enforceOwnership`) y ADMIN (bypass). `/api/carts/**` y `/api/addresses/**` admiten USER y ADMIN. `/api/audit-logs/**` es solo ADMIN. Esto previene escalada de privilegios (un USER no puede ascender a otro a ADMIN ni borrar el catálogo).
- **Validación de `unitPrice` en `OrderService.create`**: el `unitPrice` se lee SIEMPRE de `Product.price` (la BD), nunca del body. Un cliente no puede comprar productos a un precio distinto al real.
- **Revertir stock en cancel**: si un `Order` se cancela estando en `PAID`, se devuelve el stock a `Product.stock` automáticamente (mismo número de unidades por cada item).
- **Audit log registrado tras commit**: `AuditLogService.record(...)` usa `TransactionSynchronizationManager.registerSynchronization(...).afterCommit()` para evitar "DELETEs fantasma" en el log cuando una operación falla al hacer commit. Si la transacción externa hace rollback, el log no se persiste.

## 12. Roadmap (v1)

- **Admin**: gestión productos, categorías, stock, pedidos, clientes, dashboard.
- **Clientes**: registro, login, perfil, direcciones, historial, carrito, pedidos.
- **Métricas**: ventas totales/por mes/por categoría, productos más vendidos, clientes más activos, ticket medio, pedidos pendientes, evolución de ingresos.
- **Hecho**: entidad `User` con `UserDetailsService` DB-backed (`DbUserDetailsService`), entidad `Address`, entidad `Order` + `OrderItem` + `Payment` + `Shipment`, entidad `Cart` + `CartItem` con checkout, `AuditLog` append-only con `Action` enum y filtros paginados, stock real validado y descontado en `OrderService.pay()`, Swagger UI 3.0.3 con JWT bearer.
- **Pendiente**: integración real con un payment provider (Stripe/PayPal) para `Payment.providerReference` (actualmente `providerReference` es un String libre que se acepta en el body), webhooks de shipment, persistencia de `Cart` para carritos abandonados (`status=ABANDONED` con job que lo asigne tras X días sin actividad), la API de `AuditLog` con `ipAddress` y `actorRole` (hoy no se loguea la IP del cliente y solo se loguean acciones de ADMIN), tests de concurrencia (dos checkouts simultáneos del mismo producto con stock justo), hardening de CORS para entornos LAN (el default de `application-prod.properties` es `http://localhost`).

## 13. Cómo se conecta la aplicación

### 13.1 Topología de despliegue (Docker)

```
┌────────────────────────────────────────────────────────────┐
│ Navegador (http://localhost)                              │
└──────────────────────┬─────────────────────────────────────┘
                       │ HTTP
                       ▼
┌────────────────────────────────────────────────────────────┐
│ Frontend — Nginx (puerto 80)                              │
│   Sirve /  → /usr/share/nginx/html (Vite build)           │
│   Proxy   /api/* → http://backend:8080  (mismo origen)    │
└──────────────────────┬─────────────────────────────────────┘
                       │ HTTP /api/...
                       ▼
┌────────────────────────────────────────────────────────────┐
│ Backend — Spring Boot (puerto 8080)                       │
│   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐  │
│   │ JwtAuth      │──→│ Controller   │──→│ Service      │  │
│   │ Filter       │   │ (REST)       │   │ (lógica)     │  │
│   └──────────────┘   └──────┬───────┘   └──────┬───────┘  │
│                            │                   │          │
│                            │                   ▼          │
│                            │          ┌────────────────┐  │
│                            │          │ GlobalException│  │
│                            │          │ Handler        │  │
│                            │          └────────────────┘  │
│                            ▼                              │
│                    ┌──────────────┐                       │
│                    │ JPA /        │                       │
│                    │ Hibernate    │                       │
│                    └──────┬───────┘                       │
└───────────────────────────┼──────────────────────────────┘
                            │ JDBC
                            ▼
┌────────────────────────────────────────────────────────────┐
│ MySQL 8 (puerto 3306)                                     │
│   silvalde_web_db — 8 tablas:                              │
│   users, categories, products, addresses,                 │
│   orders, order_items, payments, shipments                │
└────────────────────────────────────────────────────────────┘
```

### 13.2 Flujo de una request típica (cliente lista sus pedidos)

```
1. Browser:    GET http://localhost/api/orders
2. Nginx:      proxy_pass http://backend:8080/api/orders
3. Backend:    Spring Security filter chain:
                 a. JwtAuthenticationFilter extrae "Authorization: Bearer <token>"
                 b. jwtService.extractUsername() → "customer@example.com"
                 c. dbUserDetailsService.loadUserByUsername() → UserDetails con ROLE_USER
                 d. SecurityContextHolder.setAuthentication(...)  (la request ya es "autenticada")
4. Backend:    DispatcherServlet → OrderController.list(status=null, authentication)
5. Controller: AuthUtils.currentUser(authentication, userRepository) → User(id=2)
               AuthUtils.isAdmin(authentication) → false
               log.info("GET /api/orders userId=2 isAdmin=false status=null")
6. Controller: orderService.list(userId=2, isAdmin=false, status=null)
7. Service:    orderRepository.findByCustomerId(2L) → SELECT * FROM orders WHERE customer_id=2
8. Service:    mapea cada Order → OrderResponse (proyecta user.id, items, payment, shipment)
9. Controller: retorna List<OrderResponse> (200 OK)
10. Jackson:   serializa a JSON
11. Nginx:     devuelve al browser
12. Browser:   fetch resuelve la Promise → React setState(orders)
```

### 13.3 Capas del backend (lo que hace cada una)

| Capa | Paquete | Responsabilidad | Lo que NO hace |
|---|---|---|---|
| **Filtros** | `config/` | Autenticación (JWT), CORS, CSRF off, sesión stateless | Lógica de negocio |
| **Controller** | `controller/<feature>/` | Recibir HTTP request, validar body (`@Valid`), llamar al service, loguear entrada/salida, devolver `ResponseEntity` o DTO | Acceso a BD directo, reglas de negocio, mapeo de entity → DTO |
| **Service** | `service/<feature>/` | Reglas de negocio (ownership, transiciones de estado, cálculo de totales), coordinar repos, mapear entity → DTO, loguear INFO/WARN/ERROR | Conocer HTTP, devolver `ResponseEntity` |
| **Repository** | `repository/<feature>/` | Queries JPA (derivadas y custom), abstraer SQL | Lógica, validaciones |
| **Model** | `model/<feature>/` | Entidades JPA con auditoría (`@CreatedDate`/`@LastModifiedDate`), enums, `@ToString.Exclude` + `@JsonIgnore` en relaciones LAZY | DTOs |
| **DTO** | `dto/<feature>/` | `record` inmutables que viajan por HTTP, con Bean Validation (`@NotBlank`, `@Size`, etc.) | Anotaciones JPA |
| **Exception** | `exception/<feature>/` y `GlobalExceptionHandler` | Tipos de error de dominio + mapeo a `ProblemDetail` (RFC 7807) con HTTP status correcto | Try/catch en controllers |
| **Config** | `config/` | Beans (Security, JWT, OpenAPI, AuthUtils, DataSeeder) | Lógica |

### 13.4 Modelo de datos relacional (a día de hoy)

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
              │                                   │
              ▼                                   ▼
       ┌──────────┐         ┌──────────┐   (FK lógica)
       │ products │◄────────│categories│
       └──────────┘  (N:1)  └──────────┘

                    ┌────────────┐
                    │ audit_logs │ ── (N:1) actor_id ──► users
                    │ (append-   │
                    │  only)     │  entity_name + entity_id
                    └────────────┘      apuntan a CUALQUIER entity

       Leyenda: ─── FK JPA ─── FK lógica
```

Notas:
- `Order.customer_id`, `Address.user_id` y `Cart.customer_id` son **FKs JPA** (`@ManyToOne` / `@OneToOne` con `unique=true`, validadas por Hibernate y con `ON DELETE` no restrictivo en MySQL).
- `OrderItem.product_id` y `CartItem.product_id` son **FKs lógicas** (un `Long` sin `@ManyToOne`) para mantener las entities ligeras. Si borras un `Product` referenciado, los items quedan con un id huérfano — defensa en profundidad con `@JsonIgnore` en la respuesta para no romper la API. En `CartItem` esto se mitiga con la validación de `active` y `stock` en `checkout`.
- `payments.order_id` y `shipments.order_id` son FKs JPA 1:1 con `unique=true`, cascade ALL + orphanRemoval.
- La relación inversa (User → List<Address>, User → List<Order>, User → Cart) está **explícitamente no mapeada** para mantener la BD limpia y evitar N+1. Si más adelante el admin necesita "todos los pedidos de un usuario", se hace con `findByCustomerId(Long)` en el repository (ya existe).

### 13.5 Cómo encajará Cart (próxima feature)

**Hecho.** Cart implementado como `Cart` (1:1 con `User`, `customer_id` UNIQUE) + `CartItem` (N:1 con `Cart`, cascade ALL + orphanRemoval). La diferencia clave con `OrderItem`: **no** persiste `unitPrice` ni `lineTotal`. Cada `GET /api/carts` re-lee los precios actuales de `Product` con un `findAllById` batch. Esto significa que el usuario ve el precio real SIEMPRE (incluso si cambió desde que añadió el item).

| Aspecto | Cart | Order |
|---|---|---|
| **Estado** | `ACTIVE` / `ABANDONED` / `CONVERTED` (se vacía al hacer checkout) | `PENDING → PAID → SHIPPED → DELIVERED` (o `CANCELLED`) |
| **Total** | Recalculado en cada GET desde `Product.price` actual | Snapshot inmutable (`unitPrice` congelado) |
| **Duración** | Persiste hasta checkout o limpieza manual | Inmutable una vez entregado/cancelado |
| **Stock** | Validado en `checkout` (no reservado) | Descontado en `pay()` (futuro) |
| **Relación con User** | 1 cart activo por user (`UNIQUE` en `customer_id`) | N orders por user |

**Flujo de checkout** (lo que hace `CartService.checkout(...)`):

```
1. Cliente hace POST /api/carts/checkout con { shippingAddress }
2. Backend carga el cart activo del user (findByCustomerId o crea)
3. Backend hace findAllById(productsIds) para batch-load de Product
4. Backend valida que cada Product existe, está activo, y tiene stock >= quantity
   - Si falla stock → InsufficientStockException → 409 con {productId, requested, available}
5. Backend construye OrderItemRequest[] con Product.price actual
6. Backend llama a orderService.create(userId, isAdmin, new OrderCreateRequest(items, shippingAddress))
7. Backend vacía el cart y marca status=CONVERTED
8. Backend devuelve el OrderResponse (PENDING) al cliente
9. Cliente hace PATCH /api/orders/{id}/pay como cualquier otro pedido
```

Este flujo **no** duplica lógica: `CartService.checkout` es un wrapper puro sobre `OrderService.create(...)`. Si mañana eliminamos Cart del frontend, el cliente puede seguir creando orders vía `POST /api/orders` directamente.

**Endpoints finales** (todos USER/ADMIN, no DELETE masivos):

```
GET    /api/carts                  → mi cart activo (crea si no existe)
POST   /api/carts/items            → { productId, quantity }    añade o mergea
PATCH  /api/carts/items/{itemId}   → { quantity }
DELETE /api/carts/items/{itemId}   → borra item
DELETE /api/carts                  → vacía el cart (no lo borra, status vuelve a ACTIVE)
POST   /api/carts/checkout         → { shippingAddress }         crea Order PENDING
```

**Lo que NO se hace todavía (TODO):**
- Decrementar `Product.stock` en `pay()` (validamos en checkout pero no descontamos).
- Cron job que marque carts como `ABANDONED` tras X días sin actividad.
- Persistir el `unitPrice` que vio el usuario al añadir el item (para mostrar "antes X, ahora Y" en el frontend si hubo cambio de precio).

### 13.6 Cómo encajará el frontend (en una iteración futura)

```
┌────────────────────────────────────────────────────────────┐
│ React 19 (Vite 8, Tailwind v4)                            │
│                                                            │
│  main.jsx → BrowserRouter → App.jsx                       │
│      │                                                     │
│      ├── /                 Home (catálogo público)         │
│      ├── /products/:id     Detalle producto                │
│      ├── /cart             Cart (requiere login)           │
│      ├── /checkout         Confirmar envío + pago          │
│      ├── /orders           Mis pedidos                     │
│      ├── /orders/:id       Detalle pedido                  │
│      ├── /login            Login                           │
│      └── /admin/*          Panel admin (solo ADMIN)        │
│                                                            │
│  src/api/                                                  │
│    ├── client.js          fetch wrapper, baseURL, JWT      │
│    ├── auth.js            login(), logout(), me()          │
│    ├── products.js        list, get, create, update        │
│    ├── cart.js            get, addItem, updateItem,        │
│    │                      removeItem, clear, checkout      │
│    └── orders.js          list, get, pay, cancel           │
│                                                            │
│  src/store/                                                │
│    ├── authContext.jsx    user, token, login(), logout()   │
│    └── cartContext.jsx    cart, addItem(), removeItem()    │
└────────────────────────────────────────────────────────────┘
```

Punto de entrada HTTP (frontend → backend):
- **Docker** (prod): mismas rutas relativas `/api/...` (Nginx hace de proxy, mismo origen).
- **Dev local** (`npm run dev` en `:5173`): rutas absolutas `http://localhost:8080/api/...` con CORS abierto en `application-local.properties` para `http://localhost:5173`.

**El `cartContext.jsx`** se monta así:
1. Al login, hace `GET /api/carts` y guarda el `CartResponse` en estado.
2. `addItem` → `POST /api/carts/items` → actualiza el estado con la respuesta (que ya trae el item nuevo con `unitPrice` y `lineTotal` recalculados).
3. `checkout(shippingAddress)` → `POST /api/carts/checkout` → recibe el `OrderResponse`, navega a `/orders/{id}`.
4. Tras `pay`, el frontend ya no toca el cart: la próxima vez que se llame `GET /api/carts`, el server lo devolverá vacío (status=`CONVERTED` → `ACTIVE` tras `clear` o sigue `CONVERTED` mostrando el último pedido).

### 13.7 Verificación rápida del estado actual

```bash
# Backend levanta y crea el schema con ddl-auto=update
docker compose up -d backend
docker compose logs -f backend | grep "Started BackendApplication"

# Frontend sirviendo el bundle
docker compose up -d frontend
curl -I http://localhost

# Swagger UI accesible sin auth
open http://localhost:8080/swagger-ui.html

# Login + request autenticada
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"TuPassword"}' | jq -r .token)

curl -s http://localhost:8080/api/orders -H "Authorization: Bearer $TOKEN" | jq .
```

Si los cuatro comandos anteriores funcionan, el grafo entero (DB → JPA → Service → Controller → JwtFilter → Nginx → browser) está vivo.
