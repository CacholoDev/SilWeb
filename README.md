## Silvalde Web

Tienda online + panel de administración para un negocio de hogar. Monorepo con backend Spring Boot y frontend React, orquestados con Docker.

## Estado actual

- **Backend**: API REST con Spring Boot 4 + JPA + Spring Security (JWT) + MySQL. CRUD de `categories`, `products` y `users`. Logger SLF4J en services, controllers y filtros. Perfiles `local` y `prod`. DevTools para live reload.
- **Frontend**: scaffold React 19 + Vite + Tailwind v4. Sin router ni páginas todavía; pendiente construir UI.
- **Infra**: Docker Compose con `mysql`, `backend` y `frontend` (Nginx). Nginx hace proxy de `/api/*` al backend para evitar CORS en producción.

## Documentación

- `doc.md`: notas técnicas (stack, arquitectura, modelo de datos, API REST, dev local, seguridad, roadmap).
- `AGENTS.md`: normas de trabajo y convenciones del repo (paquetes, logging, gotchas, skills).
- `frontend/README.md`: boilerplate por defecto de Vite, ignorable.

## Arranque rápido con Docker

Desde la raíz del proyecto:

```bash
docker compose up --build
```

Servicios expuestos:
- Frontend (Nginx): `http://localhost`
- Backend: `http://localhost:8080`
- MySQL: `localhost:3306`

Login por defecto (en `.env`):
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<APP_ADMIN_PASSWORD>"}'
```

## Dev local sin Docker

Para iterar con hot reload en backend:

```bash
# 1. Levantar MySQL y frontend en Docker
docker compose up mysql frontend -d

# 2. Asegúrate de tener SPRING_PROFILES_ACTIVE=local en .env

# 3. Backend local
cd backend && ./mvnw spring-boot:run

# Frontend con HMR
cd frontend && npm run dev   # puerto 5173
```

## Estructura

- `backend/`: API Spring Boot + `Dockerfile` + `docker-compose.yml` (alternativa).
- `frontend/`: SPA React + `Dockerfile` + `nginx.conf`.
- `docker-compose.yml` (raíz): stack completo.
- `.env` / `.env.example`: secretos y perfil activo.

## Stack

- **Backend**: Java 21, Spring Boot 4.0.6, Spring Data JPA, Spring Security, JJWT 0.12.5, Lombok, MySQL 8 (prod) / H2 (tests).
- **Frontend**: React 19, Vite 8, JavaScript (sin TS), Tailwind v4.
- **Infra**: Docker, Nginx.

## Roadmap (v1)

- **Admin**: gestión de productos, categorías, stock, pedidos, clientes, dashboard.
- **Clientes**: registro, login, perfil, direcciones, historial, carrito, pedidos.
- **Métricas**: ventas totales/por mes/por categoría, productos más vendidos, clientes más activos, ticket medio, pedidos pendientes, evolución de ingresos.
