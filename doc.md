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

## 9. API REST

Todas las rutas requieren `Authorization: Bearer <token>` excepto `POST /api/auth/login`. Respuestas de error son `ProblemDetail` JSON.

### Auth
- `POST /api/auth/login` — body `{username, password}` → `{token, username, roles, tokenType}`.

### Categories (`/api/categories`)
- `POST` crear · `GET` listar (`?active=`) · `GET /{id}` detalle · `PUT /{id}` actualizar · `DELETE /{id}` eliminar.

### Products (`/api/products`)
- `POST` crear · `GET` listar (`?active=&categoryId=`) · `GET /{id}` detalle · `PUT /{id}` actualizar · `DELETE /{id}` eliminar.

### Users (`/api/users`)
- `POST` crear (hashea password) · `GET` listar (`?active=&role=`) · `GET /{id}` detalle · `PUT /{id}` actualizar (password opcional) · `DELETE /{id}` eliminar.

### Addresses (`/api/addresses`)
- `POST` crear · `GET` listar (`?userId=`) · `GET /{id}` detalle · `PUT /{id}` actualizar · `DELETE /{id}` eliminar.

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
- **Infra pendiente**: migrar `UserDetailsService` de in-memory a DB (entidad `User`), añadir Address, Order, Cart.
