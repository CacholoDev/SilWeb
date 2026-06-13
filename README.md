# Silvalde Web

Tienda online + panel de administración para un negocio de hogar. Monorepo con **backend Spring Boot 4** + **frontend React 19** orquestados con Docker Compose.

## Estado actual (v0.0.1-SNAPSHOT)

| Módulo | Qué hay | Qué falta |
|---|---|---|
| **Backend** | API REST de `users`, `categories`, `products`, `addresses`, `carts`, `orders`, `audit-logs`, `auth`. JWT, BCrypt, Spring Security con matchers por rol. Stock real (descontado en `pay()`, restaurado en `cancel()`). Audit log append-only. Swagger UI 3.0.3. Actuator + healthcheck. Rate limiting en `/api/auth/login`. Seed endpoint `/api/dev/seed`. Tests 117/117. | Paginación en list endpoints. Pagos reales (Stripe). Refund con reverter stock. |
| **Frontend** | Vite + React 19 + Tailwind v4. Sin router ni páginas todavía. | Toda la UI: landing, login, catálogo, cart, checkout, orders, admin. |
| **Infra** | Docker Compose con `mysql` + `backend` + `frontend` (Nginx proxy `/api/*` → backend). `.dockerignore` en backend y frontend. BuildKit cache mounts. Non-root user. healthchecks. | CI, k8s, TLS, observability stack. |

## Arranque rápido

### 1. Configurar el `.env` (una vez)

```bash
cp .env.example .env
```

Edita `.env` y rellena **obligatorio**:

```env
JWT_SECRET=<genera con `openssl rand -base64 64`>           # ≥32 bytes
APP_ADMIN_PASSWORD=<genera con `openssl rand -base64 24`>  # ≥12 chars
APP_CUSTOMER_PASSWORD=<genera con `openssl rand -base64 24`> # ≥12 chars
```

El backend **no arranca** si los valores siguen siendo los placeholders. Es fail-fast por seguridad.

### 2. Levantar todo

```bash
docker compose up --build
```

Servicios expuestos:

- Frontend: `http://localhost`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- MySQL: `localhost:3306`

### 3. Sembrar la BD con datos de ejemplo

El `DataSeeder` (en cada arranque) crea los usuarios admin y customer. Para tener también categorías y productos:

```bash
curl -X POST http://localhost:8080/api/dev/seed
```

Es idempotente: si las categorías/productos ya existen, no duplica. Crea 4 categorías, 8 productos y 1 dirección de ejemplo para el customer.

### 4. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"<APP_ADMIN_PASSWORD_de_tu_.env>"}'
```

Respuesta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "admin@example.com",
  "roles": ["ROLE_ADMIN"],
  "tokenType": "Bearer"
}
```

Para el customer, cambia el email. Ver `docs/POSTMAN.md` para la guía completa de la API.

## Dev local sin Docker

```bash
docker compose up mysql -d
cd backend && ./mvnw spring-boot:run   # SPRING_PROFILES_ACTIVE=local en .env
cd frontend && npm run dev             # puerto 5173
```

## Tests

```bash
cd backend && ./mvnw test
# 117 tests, ~10s
```

Para los integration tests (incluye el context loads completo contra H2):

```bash
cd backend && ./mvnw test -Dgroups=integration
```

## Documentación

- **`doc.md`** (raíz): notas técnicas (arquitectura, modelo de datos, API REST, dev local, seguridad, roadmap).
- **`AGENTS.md`** (raíz): normas de trabajo y convenciones del repo.
- **`docs/POSTMAN.md`**: guía paso a paso para probar la API con Postman o curl.
- **`frontend/README.md`**: placeholder.

## Stack

- **Backend**: Java 21, Spring Boot 4.0.6, JPA, Spring Security, JJWT 0.12.5, springdoc 3.0.3, bucket4j 8.10.1, Lombok, MySQL 8 (prod) / H2 (tests).
- **Frontend**: React 19, Vite 8, JavaScript, Tailwind v4, axios (próximo), react-router-dom 7 (próximo).
- **Infra**: Docker, Nginx, BuildKit.

## Roadmap v1

- **Admin**: CRUD productos/categorías/usuarios/pedidos, dashboard de métricas, exportación CSV.
- **Clientes**: landing, catálogo, búsqueda, filtros, ficha de producto, carrito persistente, checkout, mis pedidos con timeline, perfil, direcciones.
- **Métricas**: ventas por mes/categoría, top productos, ticket medio, cohortes, LTV.

## Licencia

Privado.
