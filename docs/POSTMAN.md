# Postman — Guía de pruebas del backend Silvalde Web

Esta guía cubre la prueba manual de **toda la API REST** desde Postman (o `curl`), en el orden lógico de uso. Asume que el backend está corriendo en `http://localhost:8080` (Docker, `mvnw spring-boot:run`, o VS Code).

> Si prefieres UI interactiva: `http://localhost:8080/swagger-ui.html` (cubre lo mismo,Swagger UI 3.0.3 con soporte JWT bearer).

---

## 0. Configuración inicial

### 0.1 Variables de entorno (Postman → pestaña "Variables")

| Variable | Valor |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `adminToken` | *(vacío, se rellena tras el login admin)* |
| `customerToken` | *(vacío, se rellena tras el login customer)* |

### 0.2 Authorization

Todas las requests (excepto `POST /api/auth/login`) necesitan JWT en cabecera:

```
Authorization: Bearer {{adminToken}}      # para probar como admin
Authorization: Bearer {{customerToken}}   # para probar como cliente
```

En Postman: pestaña **Authorization** → Type: **Bearer Token** → Token: `{{adminToken}}` o `{{customerToken}}`.

### 0.3 Content-Type

Todas las requests con body: `Content-Type: application/json`.

---

## 1. Autenticación (sin token)

### 1.1 Login admin

```
POST {{baseUrl}}/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@example.com",
  "password": "SilwebDBadmin104/ññ"
}
```

> Usa la password real que tengas en `.env` (variable `APP_ADMIN_PASSWORD`). Si la cambiaste, pon esa.

**Respuesta 200** (copia `token` en `adminToken`):

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "admin@example.com",
  "roles": ["ROLE_ADMIN"],
  "tokenType": "Bearer"
}
```

### 1.2 Login cliente

```
POST {{baseUrl}}/api/auth/login
```

```json
{
  "email": "customer@example.com",
  "password": "SilwebDBCust104/ññ"
}
```

Respuesta 200 — copia `token` en `customerToken`.

### 1.3 Verificar 401 con password mal

```json
{
  "email": "customer@example.com",
  "password": "WRONG"
}
```

Debe devolver **401** con `ProblemDetail`:
```json
{
  "title": "Authentication failed",
  "detail": "Invalid username or password.",
  "status": 401,
  "path": "/api/auth/login",
  "timestamp": "..."
}
```

### 1.4 Verificar 400 con email mal formado

```json
{ "email": "no-es-email", "password": "cualquiera" }
```

Debe devolver **400** con `errors[0].field = "email"`.

---

## 2. Categorías (necesario para crear productos)

Todas con `Authorization: Bearer {{adminToken}}`.

### 2.1 Crear categoría

```
POST {{baseUrl}}/api/categories
```

```json
{ "name": "Hogar", "description": "Productos para el hogar" }
```

**201 Created** → guarda `id` (lo necesitas para el producto).
```json
{ "id": 1, "name": "Hogar", "description": "...", "active": true, "createdAt": "...", "updatedAt": "..." }
```

### 2.2 Listar categorías

```
GET {{baseUrl}}/api/categories
GET {{baseUrl}}/api/categories?active=true
```

### 2.3 Detalle

```
GET {{baseUrl}}/api/categories/1
```

### 2.4 Actualizar

```
PUT {{baseUrl}}/api/categories/1
```

```json
{ "name": "Hogar y Cocina", "description": "Hogar, cocina y menaje", "active": true }
```

### 2.5 Borrar

```
DELETE {{baseUrl}}/api/categories/1
```

Debe devolver **204 No Content** (body vacío).

---

## 3. Productos (necesario para crear pedidos)

Con `Authorization: Bearer {{adminToken}}`.

### 3.1 Crear producto

```
POST {{baseUrl}}/api/products
```

```json
{
  "name": "Set de sartenes antiadherentes",
  "sku": "SAR-ANTI-001",
  "description": "Juego de 3 sartenes 20/24/28 cm",
  "price": 49.99,
  "stock": 100,
  "active": true,
  "categoryId": 1
}
```

**201 Created** → guarda `id` (lo necesitas para los pedidos).

### 3.2 Crear segundo producto (para probar varios items)

```json
{
  "name": "Cafetera italiana 6 tazas",
  "sku": "CAF-ITA-006",
  "description": "Aluminio, apta para inducción con adaptador",
  "price": 24.50,
  "stock": 50,
  "active": true,
  "categoryId": 1
}
```

### 3.3 Listar

```
GET {{baseUrl}}/api/products
GET {{baseUrl}}/api/products?active=true&categoryId=1
```

### 3.4 Detalle

```
GET {{baseUrl}}/api/products/1
```

### 3.5 Actualizar (subir precio → importante: los pedidos ya existentes NO se actualizan porque `unitPrice` es snapshot)

```
PUT {{baseUrl}}/api/products/1
```

```json
{
  "name": "Set de sartenes antiadherentes",
  "sku": "SAR-ANTI-001",
  "description": "Juego de 3 sartenes 20/24/28 cm, mango ergonómico",
  "price": 59.99,
  "stock": 100,
  "active": true,
  "categoryId": 1
}
```

### 3.6 Borrar

```
DELETE {{baseUrl}}/api/products/2
```

---

## 4. Direcciones (necesario para tener un cliente con dirección)

Con `Authorization: Bearer {{customerToken}}` (probar también con `adminToken`).

### 4.1 Crear dirección para el cliente (id 2)

```
POST {{baseUrl}}/api/addresses
```

```json
{
  "fullName": "Cliente Demo",
  "street": "Calle Mayor 1, 2ºA",
  "city": "Madrid",
  "province": "Madrid",
  "postalCode": "28001",
  "country": "España",
  "isDefault": true,
  "userId": 2
}
```

### 4.2 Listar mis direcciones

```
GET {{baseUrl}}/api/addresses?userId=2
```

### 4.3 Verificar validación de postal code

```json
{
  "fullName": "Test",
  "street": "X",
  "city": "X",
  "province": "X",
  "postalCode": "BAD",
  "country": "España",
  "isDefault": false,
  "userId": 2
}
```

**400** con `errors[0].field = "postalCode"`.

---

## 5. Pedidos (Orders) — CRUD completo

Todo desde `Authorization: Bearer {{customerToken}}` salvo que se indique.

### 5.1 Crear pedido (PENDING)

```
POST {{baseUrl}}/api/orders
```

```json
{
  "items": [
    { "productId": 1, "quantity": 2, "unitPrice": 49.99 },
    { "productId": 2, "quantity": 1, "unitPrice": 24.50 }
  ],
  "shippingAddress": "Calle Mayor 1, 2ºA, 28001 Madrid, España"
}
```

**201 Created** — guarda `id` y `orderNumber`:

```json
{
  "id": 1,
  "orderNumber": "ORD-20260609-1234",
  "customerId": 2,
  "status": "PENDING",
  "total": 124.48,
  "shippingAddress": "...",
  "items": [
    { "id": 1, "productId": 1, "quantity": 2, "unitPrice": 49.99, "lineTotal": 99.98 },
    { "id": 2, "productId": 2, "quantity": 1, "unitPrice": 24.50, "lineTotal": 24.50 }
  ],
  "payment": null,
  "shipment": null,
  "createdAt": "...",
  "updatedAt": "..."
}
```

> El `total` lo calcula el server (49.99 × 2 + 24.50 × 1 = 124.48). Si mandas `total` en el body, se ignora.

### 5.2 Crear pedido sin items (debe fallar 400)

```json
{ "items": [], "shippingAddress": "X" }
```

**400** con `errors[0].field = "items"`.

### 5.3 Crear pedido con productId inexistente (debe fallar 404)

```json
{
  "items": [ { "productId": 9999, "quantity": 1, "unitPrice": 10.00 } ],
  "shippingAddress": "X"
}
```

**404** con `title: "Product not found"`.

### 5.4 Listar MIS pedidos

```
GET {{baseUrl}}/api/orders
GET {{baseUrl}}/api/orders?status=PENDING
```

Solo devuelve los del `customerId` extraído del JWT. No ve pedidos de otros.

### 5.5 Listar como admin (ve todos)

```
GET {{baseUrl}}/api/orders
Authorization: Bearer {{adminToken}}
```

### 5.6 Detalle de mi pedido

```
GET {{baseUrl}}/api/orders/1
```

### 5.7 Detalle de pedido ajeno (debe fallar 403)

```
GET {{baseUrl}}/api/orders/1
Authorization: Bearer {{adminToken}}  // admin sí puede
```

Para forzar 403: crea otro usuario, crea un pedido con ese usuario, intenta verlo con el `customerToken` → **403 Access denied**.

### 5.8 Actualizar dirección de envío (solo en PENDING)

```
PUT {{baseUrl}}/api/orders/1
```

```json
{ "shippingAddress": "Calle Nueva 99, 28002 Madrid" }
```

---

## 6. Transiciones de estado de Order

### 6.1 Pagar (PENDING → PAID)

```
PATCH {{baseUrl}}/api/orders/1/pay
```

```json
{
  "method": "CARD",
  "providerReference": "stripe_ch_3O5xQ2Kdjv"
}
```

**200 OK** — el campo `payment` aparece populado, `status` = `"PAID"`.

Métodos válidos: `CARD`, `PAYPAL`, `BANK_TRANSFER`, `CASH_ON_DELIVERY`.

### 6.2 Intentar pagar dos veces (debe fallar 422)

```
PATCH {{baseUrl}}/api/orders/1/pay
```

```json
{ "method": "CARD" }
```

**422 Unprocessable Entity** con `title: "Order invalid state"`, `detail: "Only PENDING orders can be paid. Current status: PAID"`.

### 6.3 Intentar enviar antes de pagar (debe fallar 422)

Crea otro pedido nuevo (repite 5.1), luego:

```
PATCH {{baseUrl}}/api/orders/2/ship
```

**422** con `detail: "Only PAID orders can be shipped. Current status: PENDING"`.

### 6.4 Enviar (PAID → SHIPPED)

Sobre el pedido 1 (ya PAID):

```
PATCH {{baseUrl}}/api/orders/1/ship
```

```json
{
  "carrier": "SEUR",
  "trackingNumber": "TRACK-123456"
}
```

**200 OK** — `status` = `"SHIPPED"`, `shipment` populado.

### 6.5 Marcar como entregado (SHIPPED → DELIVERED)

```
PATCH {{baseUrl}}/api/orders/1/deliver
```

Body: **ninguno** (POST sin body).

**200 OK** — `status` = `"DELIVERED"`, `shipment.status` = `"DELIVERED"`, `shipment.deliveredAt` con timestamp.

### 6.6 Cancelar un pedido PENDING

Crea el pedido 3 (repite 5.1), luego:

```
PATCH {{baseUrl}}/api/orders/3/cancel
```

**200 OK** — `status` = `"CANCELLED"`.

### 6.7 Intentar cancelar un pedido ya enviado (debe fallar 422)

```
PATCH {{baseUrl}}/api/orders/1/cancel
```

**422** con `detail: "Cannot cancel an order in status DELIVERED"`.

### 6.8 Borrar pedido (solo ADMIN)

```
DELETE {{baseUrl}}/api/orders/3
Authorization: Bearer {{adminToken}}
```

**204 No Content**.

Si lo intentas con `{{customerToken}}` → **403 Access denied** (matcher en SecurityConfig).

---

## 7. Tests de validación de los DTOs

### 7.1 Carrier obligatorio al enviar

```
PATCH {{baseUrl}}/api/orders/2/ship
```

```json
{ "carrier": "", "trackingNumber": "" }
```

**400** con `errors[0].field = "carrier"`.

### 7.2 Quantity ≥ 1

Crear un Order con un item de `quantity: 0` → **400** con `errors[0].field = "items[0].quantity"`.

### 7.3 unitPrice > 0

Item con `unitPrice: 0` o negativo → **400** con `errors[0].field = "items[0].unitPrice"`.

---

## 8. Verificación del flujo completo de un cliente

Esta es la **ruta crítica** que debería funcionar end-to-end:

| Paso | Endpoint | Status esperado |
|---|---|---|
| 1. Login cliente | `POST /api/auth/login` | 200 + token |
| 2. Listar mis pedidos (vacío) | `GET /api/orders` | 200 + `[]` |
| 3. Crear pedido | `POST /api/orders` | 201 + `status: PENDING` |
| 4. Verificar total calculado | inspect response | `total` = `sum(lineTotal)` |
| 5. Pagar | `PATCH /api/orders/{id}/pay` | 200 + `status: PAID` + `payment` populado |
| 6. Enviar | `PATCH /api/orders/{id}/ship` | 200 + `status: SHIPPED` + `shipment` populado |
| 7. Entregar | `PATCH /api/orders/{id}/deliver` | 200 + `status: DELIVERED` + `shipment.deliveredAt` |
| 8. Listar pedidos (filtro) | `GET /api/orders?status=DELIVERED` | 200 + tu pedido |
| 9. Ver detalle final | `GET /api/orders/{id}` | 200 + todo el árbol |

---

## 9. Verificación del flujo de admin

| Paso | Endpoint | Status esperado |
|---|---|---|
| 1. Login admin | `POST /api/auth/login` | 200 + token |
| 2. Ver todos los pedidos | `GET /api/orders` | 200 + pedidos de todos |
| 3. Ver pedido de otro user | `GET /api/orders/1` (siendo de customer) | 200 (ADMIN bypass) |
| 4. Borrar un pedido | `DELETE /api/orders/1` | 204 |
| 5. Intentar ver pedido borrado | `GET /api/orders/1` | 404 |

---

## 10. Errores esperados (referencia rápida)

| Escenario | HTTP | Title del ProblemDetail |
|---|---|---|
| Login con password mal | 401 | Authentication failed |
| Body con email no-tld | 400 | Validation error |
| Body con `items: []` | 400 | Validation error |
| Item con `productId` inexistente | 404 | Product not found |
| `GET /api/orders/999` | 404 | Order not found |
| `GET /api/orders/{id}` siendo de otro user | 403 | Access denied |
| `PATCH /pay` sobre pedido ya pagado | 422 | Order invalid state |
| `PATCH /cancel` sobre pedido entregado | 422 | Order invalid state |
| `POST /api/orders` sin Authorization | 401 | (filter, no ProblemDetail) |
| `DELETE /api/orders/1` con `customerToken` | 403 | Access denied |
| Cualquier endpoint con token inválido | 401 | (filter, no ProblemDetail) |
| Backend caído | ECONNREFUSED | (no llega a Spring) |

---

## 11. Tips de Postman

- **Tests tab**: añade un test que guarde el token automáticamente tras el login:
  ```js
  pm.test("save token", () => {
    const json = pm.response.json();
    pm.collectionVariables.set("adminToken", json.token);
  });
  ```
- **Collection variables** vs **Environment variables**: usa Collection si solo tienes un entorno, Environment si tienes `local` + `prod` separados.
- **Save as example** en cada response para tener fixtures rápidos.
- **Newman** (CLI de Postman) puede correr toda esta suite en CI. Exporta la collection y corre `newman run collection.json`.
