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
- `PATCH /api/orders/{id}/pay` (USER/ADMIN) pagar — body `{method, providerReference}`. Transición `PENDING → PAID`. Solo owner o ADMIN.
- `PATCH /api/orders/{id}/ship` (USER/ADMIN) enviar — body `{carrier, trackingNumber}`. Transición `PAID → SHIPPED`. Solo owner o ADMIN.
- `PATCH /api/orders/{id}/deliver` (USER/ADMIN) marcar entregado — transita `SHIPPED → DELIVERED` y actualiza `shipment.deliveredAt`.
- `PATCH /api/orders/{id}/cancel` (USER/ADMIN) cancelar — solo si está en `PENDING` o `PAID`. Bloqueado en `SHIPPED`/`DELIVERED`/`CANCELLED`.
- `DELETE /api/orders/{id}` (**solo ADMIN**) eliminar.

> Reglas de autorización: el owner se obtiene SIEMPRE del JWT (`AuthUtils.currentUser` resuelve el `User` por email y compara `id` con `Order.customer.id`). Si un USER pide un pedido que no es suyo se lanza `AccessDeniedException` → 403. Los endpoints `/api/orders/**` aceptan tanto USER como ADMIN; `DELETE` es exclusivo de ADMIN vía matcher en `SecurityConfig`. Los recursos `OrderItem`/`Payment`/`Shipment` no tienen controllers propios, se acceden siempre dentro del payload de `Order`.

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

## 12. Roadmap (v1)

- **Admin**: gestión productos, categorías, stock, pedidos, clientes, dashboard.
- **Clientes**: registro, login, perfil, direcciones, historial, carrito, pedidos.
- **Métricas**: ventas totales/por mes/por categoría, productos más vendidos, clientes más activos, ticket medio, pedidos pendientes, evolución de ingresos.
- **Hecho**: entidad `User` con `UserDetailsService` DB-backed (`DbUserDetailsService`), entidad `Address`, entidad `Order` + `OrderItem` + `Payment` + `Shipment`, Swagger UI 3.0.3 con JWT bearer.
- **Pendiente**: integración real con un payment provider (Stripe/PayPal) para `Payment.providerReference`, entidad `Cart` + `CartItem`, stock real (`Product.stock -= OrderItem.quantity` al pagar, no al crear), webhooks de shipment.

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
              │ (1:N)     │ (1:N)       │ (1:N)        │
              ▼           ▼             ▼              │
       ┌──────────┐  ┌──────────┐  ┌──────────┐        │
       │addresses │  │  orders  │  │ (futuro) │        │
       └──────────┘  └────┬─────┘  │  carts   │        │
                          │        └──────────┘        │
                ┌─────────┼─────────┐                  │
                │ (1:N)   │ (1:1)   │ (1:1)            │
                ▼         ▼         ▼                  │
         ┌──────────┐┌────────┐┌──────────┐            │
         │order_    ││payments││shipments │            │
         │items     │└────────┘└──────────┘            │
         └────┬─────┘                                    │
              │ (FK lógica, no @ManyToOne)              │
              ▼                                          │
       ┌──────────┐         ┌──────────┐                │
       │ products │◄────────│categories│                │
       └──────────┘  (N:1)  └──────────┘                │
                                                       │
       Leyenda: ─── FK JPA ─── FK lógica
```

Notas:
- `Order.customer_id` y `Address.user_id` son **FKs JPA** (`@ManyToOne`, validadas por Hibernate y con `ON DELETE` no restrictivo en MySQL).
- `OrderItem.product_id` es **FK lógica** (un `Long` sin `@ManyToOne`) para mantener `OrderItem` ligero. Si borras un `Product` referenciado, el `OrderItem` queda con un id huérfano — defensa en profundidad con `@JsonIgnore` en la respuesta para no romper la API.
- `payments.order_id` y `shipments.order_id` son FKs JPA 1:1 con `unique=true`, cascade ALL + orphanRemoval.
- La relación inversa (User → List<Address>, User → List<Order>) está **explícitamente no mapeada** para mantener la BD limpia y evitar N+1. Si más adelante el admin necesita "todos los pedidos de un usuario", se hace con `findByCustomerId(Long)` en el repository (ya existe).

### 13.5 Cómo encajará Cart (próxima feature)

Cart no es un "mini-pedido": tiene semántica distinta. Diferencias que justifican una entity propia:

| Aspecto | Cart | Order |
|---|---|---|
| **Estado** | Activo / Abandonado / Convertido (no hay `PENDING → PAID`, simplemente el cart se vacía al hacer checkout) | `PENDING → PAID → SHIPPED → DELIVERED` (o `CANCELLED`) |
| **Total** | Recalculado en cada GET (los precios de `Product` pueden cambiar) | Snapshot inmutable (`unitPrice` congelado) |
| **Duración** | Persiste indefinidamente hasta que el user compra o abandona | Inmutable una vez entregado/cancelado |
| **Stock** | No reserva nada | Reservará stock al pagar (futuro) |
| **Relación con User** | 1 cart activo por user | N orders por user |

Modelo propuesto:

```java
// model/cart/Cart.java
@OneToOne(fetch = LAZY)
@JoinColumn(name = "user_id", unique = true)
private User user;                       // 1 cart activo por user (o N, con flag "active")

@OneToMany(mappedBy = "cart", cascade = ALL, orphanRemoval = true)
private List<CartItem> items;

// model/cart/CartItem.java
@ManyToOne(fetch = LAZY) @JoinColumn(name = "cart_id")
private Cart cart;
@Column(name = "product_id") private Long productId;  // FK lógica
@Column private Integer quantity;
// SIN unitPrice, SIN lineTotal → el precio se lee en vivo de Product
```

Endpoints propuestos:

```
GET    /api/carts              → mi cart activo
POST   /api/carts/items        → { productId, quantity }    añade o merge
PATCH  /api/carts/items/{id}   → { quantity }                (si 0 → borra)
DELETE /api/carts/items/{id}   → borra item
DELETE /api/carts              → vacía el cart
POST   /api/carts/checkout     → body: { shippingAddress }   CONVIERTE el cart en Order
```

**Flujo de checkout (lo importante a decidir):**

```
1. Cliente hace POST /api/carts/checkout
2. Backend crea Order con los items del cart (snapshot de Product.price en ese instante)
3. Backend borra el cart (o lo marca como "converted" para auditoría)
4. Backend devuelve el Order en estado PENDING → PATCH /pay como siempre
```

Este flujo **no** rompe Order: el `OrderCreateRequest.items[]` puede venir de dos fuentes (cart o payload manual del admin), pero el `OrderService.create(...)` recibe los `OrderItemRequest` igual. La diferencia es que `/api/carts/checkout` es un wrapper que:
1. Lee el cart
2. Convierte sus items a `OrderItemRequest`
3. Pasa al `orderService.create(...)` existente
4. Limpia el cart

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
│    ├── cart.js            get, addItem, updateQty, checkout│
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
