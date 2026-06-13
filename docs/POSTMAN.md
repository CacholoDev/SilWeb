# Postman — Guía de pruebas del backend Silvalde Web

Esta guía cubre la prueba manual de **toda la API REST** desde Postman (o `curl`), en el orden lógico de uso. Asume que el backend está corriendo en `http://localhost:8080` (Docker, `mvnw spring-boot:run`, o VS Code).

> Si prefieres UI interactiva: `http://localhost:8080/swagger-ui.html` (cubre lo mismo, Swagger UI 3.0.3 con soporte JWT bearer).

---

## 0. Configuración inicial

### 0.1 Variables de entorno (Postman → pestaña "Variables")

| Variable | Valor |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `adminToken` | *(vacío, se rellena tras el login admin)* |
| `customerToken` | *(vacío, se rellena tras el login customer)* |
| `productId1`, `productId2`, `categoryId1` | *(vacíos, se rellenan tras crear productos)* |
| `orderId` | *(vacío, se rellena tras crear un pedido)* |
| `addressId` | *(vacío, se rellena tras crear una dirección)* |

### 0.2 Authorization

Todas las requests (excepto `POST /api/auth/login` y `POST /api/dev/seed`) necesitan JWT en cabecera:

```
Authorization: Bearer {{adminToken}}      # para probar como admin
Authorization: Bearer {{customerToken}}   # para probar como cliente
```

En Postman: pestaña **Authorization** → Type: **Bearer Token** → Token: `{{adminToken}}` o `{{customerToken}}`.

### 0.3 Content-Type

Todas las requests con body: `Content-Type: application/json`.

### 0.4 Credenciales por defecto

Tras arrancar Docker (o `mvnw spring-boot:run` con el seeder), hay dos usuarios:

| Email | Rol | Password (definida en `.env`) |
|---|---|---|
| `admin@example.com` | ADMIN | `APP_ADMIN_PASSWORD` |
| `customer@example.com` | USER | `APP_CUSTOMER_PASSWORD` |

Para ver la password real:
```bash
grep APP_ .env
```

---

## 1. Sembrar la BD con datos de ejemplo (opcional pero recomendado)

Si acabas de arrancar el backend por primera vez, la BD está vacía salvo por los dos usuarios. Para tener categorías y productos con los que probar:

```
POST {{baseUrl}}/api/dev/seed
```

Sin body, sin auth (es público solo en perfiles `local` y `prod`).

**Respuesta 200** (puede ser vacío o lleno dependiendo del estado):

```json
{
  "created": ["4 categories", "8 products", "1 address for customer"],
  "productsInDb": 8,
  "categoriesInDb": 4,
  "addressesInDb": 1,
  "adminEmail": "admin@example.com",
  "customerEmail": "customer@example.com"
}
```

> Es **idempotente**: si lo llamas 5 veces, la segunda en adelante `created` viene `[]`. Si la BD ya tiene datos, no duplica.

Crea 4 categorías (Hogar, Cocina, Iluminación, Textil), 8 productos (sartenes, cafetera, lámpara, etc.), y 1 dirección de ejemplo para el customer.

---

## 2. Autenticación

### 2.1 Login admin

```
POST {{baseUrl}}/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@example.com",
  "password": "<APP_ADMIN_PASSWORD de tu .env>"
}
```

**Respuesta 200** (copia `token` en la variable `adminToken`):

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "admin@example.com",
  "roles": ["ROLE_ADMIN"],
  "tokenType": "Bearer"
}
```

> Tip Postman: en la pestaña **Tests** del request, añade:
> ```js
> pm.test("save adminToken", () => {
>   pm.collectionVariables.set("adminToken", pm.response.json().token);
> });
> ```

### 2.2 Login cliente

```
POST {{baseUrl}}/api/auth/login
```

```json
{
  "email": "customer@example.com",
  "password": "<APP_CUSTOMER_PASSWORD de tu .env>"
}
```

Copia el `token` en la variable `customerToken`.

### 2.3 Verificar 401 con password mal

```json
{ "email": "customer@example.com", "password": "WRONG" }
```

**401** con `ProblemDetail`:
```json
{ "title": "Authentication failed", "detail": "Invalid username or password.", "status": 401, ... }
```

### 2.4 Verificar 400 con email mal formado

```json
{ "email": "no-es-email", "password": "cualquiera" }
```

**400** con `errors[0].field = "email"`.

### 2.5 Verificar rate limit (11 requests rápidas)

Si disparas 11 logins en menos de 1 minuto, la 11ª recibe **429 Too Many Requests** con header `Retry-After: <segundos>`. La cuenta se resetea cada minuto por IP.

---

## 3. Categorías

Todas con `Authorization: Bearer {{adminToken}}`. Si hiciste seed, ya tienes 4.

### 3.1 Listar categorías

```
GET {{baseUrl}}/api/categories
GET {{baseUrl}}/api/categories?active=true
```

**200 OK** → array. Guarda `id=1` en `categoryId1` (la primera "Hogar").

### 3.2 Crear una categoría manualmente

```
POST {{baseUrl}}/api/categories
```

```json
{ "name": "Baño", "description": "Productos para el baño" }
```

**201 Created** → devuelve el `CategoryResponse` con el `id` autogenerado.

### 3.3 Detalle, actualizar, eliminar

```
GET {{baseUrl}}/api/categories/1
PUT {{baseUrl}}/api/categories/1
DELETE {{baseUrl}}/api/categories/1
```

`DELETE` devuelve **204 No Content** (body vacío).

### 3.4 Verificar 409 por nombre duplicado

Intenta crear otra categoría con `name: "Baño"` (mismo nombre). Devuelve **409 Conflict** con `title: "Category conflict"`.

---

## 4. Productos

`Authorization: Bearer {{adminToken}}`. Necesitas al menos una categoría (id 1) para crear productos.

### 4.1 Listar productos

```
GET {{baseUrl}}/api/products
GET {{baseUrl}}/api/products?active=true&categoryId=1
```

### 4.2 Crear producto

```
POST {{baseUrl}}/api/products
```

```json
{
  "name": "Aspirador escoba sin cable 25V",
  "sku": "ASP-ESC-25V",
  "description": "Aspirador escoba 25V, autonomía 45 min, depósito 0.8L.",
  "price": 149.90,
  "stock": 30,
  "active": true,
  "categoryId": 1
}
```

**201 Created** → guarda el `id` en `productId1`. Si haces seed ya tienes productos con id 1-8.

### 4.3 Detalle, actualizar, eliminar

```
GET {{baseUrl}}/api/products/1
PUT {{baseUrl}}/api/products/1
DELETE {{baseUrl}}/api/products/1
```

PUT body:
```json
{
  "name": "Aspirador escoba sin cable 25V (modelo 2024)",
  "sku": "ASP-ESC-25V",
  "description": "...",
  "price": 159.90,
  "stock": 25,
  "active": true,
  "categoryId": 1
}
```

### 4.4 Verificar 404 con productId inexistente

```
GET {{baseUrl}}/api/products/9999
```

**404** con `title: "Product not found"`.

### 4.5 Verificar 409 con SKU duplicado

Intenta crear otro producto con `sku: "ASP-ESC-25V"` (mismo SKU). **409** con `title: "Product conflict"`.

---

## 5. Direcciones

`Authorization: Bearer {{customerToken}}`. Si hiciste seed, el customer ya tiene una.

### 5.1 Listar mis direcciones (sin filtro de userId)

```
GET {{baseUrl}}/api/addresses
```

Como customer, solo verás las tuyas. El `userId` NO viene del body — se deriva del JWT. **Importante**: el `userId` ya no se acepta en el body (defense in depth contra IDOR).

### 5.2 Crear dirección

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
  "isDefault": true
}
```

> ⚠️ **Cuidado**: el `userId` ya no se manda. El backend lo coge del JWT. Si lo mandas, el backend lo ignora (y si lo pones en la request, es un campo extra que el DTO rechaza silenciosamente).

**201 Created** → guarda el `id` en `addressId`.

### 5.3 Verificar 400 con postal code inválido

```json
{
  "fullName": "Test",
  "street": "X",
  "city": "X",
  "province": "X",
  "postalCode": "BAD",
  "country": "España",
  "isDefault": false
}
```

**400** con `errors[0].field = "postalCode"`.

### 5.4 Verificar 403 al intentar ver la dirección de OTRO usuario (admin sí puede)

Crea otro usuario (admin):
```
POST {{baseUrl}}/api/users
Authorization: Bearer {{adminToken}}
```

```json
{
  "email": "intruso@example.com",
  "password": "StrongPassword2024!",
  "name": "Intruso",
  "phone": "600000002",
  "active": true,
  "role": "USER"
}
```

Login con él:
```
POST {{baseUrl}}/api/auth/login
```

Copia el token en `intrusoToken`. Intenta ver la dirección de customer (id de la dirección del paso 5.2):
```
GET {{baseUrl}}/api/addresses/1
Authorization: Bearer {{intrusoToken}}
```

**403 Access denied**. Como admin:
```
GET {{baseUrl}}/api/addresses/1
Authorization: Bearer {{adminToken}}
```

**200 OK** (admin bypass).

### 5.5 Actualizar y eliminar (solo las tuyas)

```
PUT {{baseUrl}}/api/addresses/{{addressId}}
DELETE {{baseUrl}}/api/addresses/{{addressId}}
```

PUT body:
```json
{
  "fullName": "Cliente Demo (actualizado)",
  "street": "Calle Nueva 5, 3ºB",
  "city": "Madrid",
  "province": "Madrid",
  "postalCode": "28002",
  "country": "España",
  "isDefault": true
}
```

---

## 6. Pedidos (Orders) — el flujo crítico

`Authorization: Bearer {{customerToken}}` salvo `DELETE /api/orders/{id}` (solo admin).

### 6.1 Crear pedido (PENDING)

```
POST {{baseUrl}}/api/orders
```

```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ],
  "shippingAddress": "Calle Mayor 1, 2ºA, 28001 Madrid, España"
}
```

> ⚠️ **Cambio importante**: el `unitPrice` **ya no se acepta en el body**. El backend lo lee siempre de `Product.price`. Si lo mandas, el DTO lo ignora (Jackson rechaza campos extra si tienes `spring.jackson.deserialization.fail-on-unknown-properties=true`, pero por ahora solo se ignoran en silencio).

**201 Created** → guarda `id` en `orderId` y `orderNumber`. El `total` lo calcula el server.

```json
{
  "id": 1,
  "orderNumber": "ORD-20260609-1234",
  "customerId": 2,
  "status": "PENDING",
  "total": 99.98,
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

> El precio unitario que ves aquí es el de `products.price` en el momento de crear el pedido. Si luego subes el precio del producto, este pedido sigue mostrando el precio viejo (snapshot inmutable).

### 6.2 Crear pedido sin items (debe fallar 400)

```json
{ "items": [], "shippingAddress": "X" }
```

**400** con `errors[0].field = "items"`.

### 6.3 Crear pedido con productId inexistente (debe fallar 404)

```json
{ "items": [ { "productId": 9999, "quantity": 1 } ], "shippingAddress": "X" }
```

**404** con `title: "Product not found"`.

### 6.4 Listar MIS pedidos

```
GET {{baseUrl}}/api/orders
GET {{baseUrl}}/api/orders?status=PENDING
```

Solo devuelve los tuyos. No ve pedidos de otros.

### 6.5 Listar como admin (ve todos)

```
GET {{baseUrl}}/api/orders
Authorization: Bearer {{adminToken}}
```

### 6.6 Detalle de mi pedido

```
GET {{baseUrl}}/api/orders/{{orderId}}
```

### 6.7 Verificar 403 al pedir pedido de otro (admin bypass)

Como customer, intenta `GET /api/orders/999` (id que no existe o de otro user) → **403 Access denied** o **404 Order not found**.

### 6.8 Actualizar dirección de envío (solo en PENDING)

```
PUT {{baseUrl}}/api/orders/{{orderId}}
```

```json
{ "shippingAddress": "Calle Nueva 99, 28002 Madrid" }
```

**200 OK**. Si el pedido ya está PAID, devuelve **422 Order invalid state**.

---

## 7. Pagar (PENDING → PAID) — el momento crítico con stock real

```
PATCH {{baseUrl}}/api/orders/{{orderId}}/pay
```

```json
{
  "method": "CARD",
  "providerReference": "stripe_ch_3O5xQ2Kdjv"
}
```

**200 OK** — `status: PAID`, `payment` populado, `product.stock` decrementado en BD.

> **Esto es la magia del stock real**: el backend valida que `product.stock >= quantity` antes de marcar PAID. Si no hay stock suficiente, devuelve **409 Insufficient stock** con `productId`, `requested`, `available` en el body. Y el stock se decrementa en la misma transacción — si algo falla, todo hace rollback.

Métodos válidos: `CARD`, `PAYPAL`, `BANK_TRANSFER`, `CASH_ON_DELIVERY`.

Verifica que el stock decrementó:
```
GET {{baseUrl}}/api/products/1
```

Verás `stock: 98` (era 100, pediste 2).

### 7.1 Intentar pagar dos veces (debe fallar 422)

```
PATCH {{baseUrl}}/api/orders/{{orderId}}/pay
```

```json
{ "method": "CARD" }
```

**422** con `detail: "Only PENDING orders can be paid. Current status: PAID"`.

### 7.2 Intentar pagar con stock insuficiente (debe fallar 409)

Crea otro pedido pidiendo 9999 unidades de un producto que solo tiene 50:
```
POST {{baseUrl}}/api/orders
```

```json
{
  "items": [ { "productId": 1, "quantity": 9999 } ],
  "shippingAddress": "X"
}
```

```
PATCH {{baseUrl}}/api/orders/<nuevo-id>/pay
```

**409** con:
```json
{
  "title": "Insufficient stock",
  "detail": "Product 1: requested 9999 but only 50 available.",
  "status": 409,
  "productId": 1,
  "requested": 9999,
  "available": 50
}
```

---

## 8. Enviar (PAID → SHIPPED)

```
PATCH {{baseUrl}}/api/orders/{{orderId}}/ship
```

```json
{
  "carrier": "SEUR",
  "trackingNumber": "TRACK-123456"
}
```

**200 OK** — `status: SHIPPED`, `shipment` populado con `carrier`, `trackingNumber`, `shippedAt`.

### 8.1 Intentar enviar un pedido que NO está PAID (debe fallar 422)

Crea otro pedido nuevo (PENDING) y:
```
PATCH {{baseUrl}}/api/orders/<otro-id>/ship
```

**422** con `detail: "Only PAID orders can be shipped. Current status: PENDING"`.

---

## 9. Marcar como entregado (SHIPPED → DELIVERED)

```
PATCH {{baseUrl}}/api/orders/{{orderId}}/deliver
```

Sin body.

**200 OK** — `status: DELIVERED`, `shipment.status: DELIVERED`, `shipment.deliveredAt: <timestamp>`.

---

## 10. Cancelar (PENDING o PAID → CANCELLED)

```
PATCH {{baseUrl}}/api/orders/{{orderId}}/cancel
```

Sin body.

**200 OK** — `status: CANCELLED`. Si estaba PAID, el stock se revierte automáticamente (ej. si pediste 2 sartenes con stock 100, ahora hay 100 otra vez).

### 10.1 Intentar cancelar un pedido ya enviado (debe fallar 422)

```
PATCH {{baseUrl}}/api/orders/{{orderId}}/cancel
```

**422** con `detail: "Cannot cancel an order in status DELIVERED"`.

---

## 11. Borrar pedido (solo ADMIN)

```
DELETE {{baseUrl}}/api/orders/{{orderId}}
Authorization: Bearer {{adminToken}}
```

**204 No Content**. Si lo intentas con `{{customerToken}}` → **403 Access denied** (matcher en SecurityConfig).

---

## 12. Carrito (Cart)

`Authorization: Bearer {{customerToken}}`.

### 12.1 Ver mi cart activo

```
GET {{baseUrl}}/api/carts
```

**200 OK** — devuelve el `CartResponse` con el cart (lo crea si no existe). Si el cart está vacío, `items: []`, `total: 0`.

### 12.2 Añadir un item

```
POST {{baseUrl}}/api/carts/items
```

```json
{ "productId": 3, "quantity": 1 }
```

**200 OK** — el cart ahora tiene el item con `unitPrice` y `lineTotal` recalculados en vivo desde `Product.price` (no es snapshot — si subes el precio, el cart muestra el nuevo).

### 12.3 Verificar el merge (suma quantity si el producto ya está)

```
POST {{baseUrl}}/api/carts/items
```

```json
{ "productId": 3, "quantity": 2 }
```

**200 OK** — el mismo item ahora tiene `quantity: 3` (1 + 2), no se duplica.

### 12.4 Cambiar la cantidad de un item del cart

```
PATCH {{baseUrl}}/api/carts/items/<itemId>
```

```json
{ "quantity": 5 }
```

### 12.5 Borrar un item del cart

```
DELETE {{baseUrl}}/api/carts/items/<itemId>
```

### 12.6 Vaciar el cart entero

```
DELETE {{baseUrl}}/api/carts
```

### 12.7 Checkout: cart → order

```
POST {{baseUrl}}/api/carts/checkout
```

```json
{ "shippingAddress": "Calle Mayor 1, 2ºA, 28001 Madrid, España" }
```

**200 OK** — devuelve el `OrderResponse` recién creado en estado `PENDING`. El cart se vacía y se marca `status: CONVERTED`.

Ahora puedes pagar el pedido con el flujo de la sección 7.

### 12.8 Intentar checkout con cart vacío (debe fallar 422)

```
POST {{baseUrl}}/api/carts/checkout
```

**422** con `detail: "Cart is empty. Add items before checkout."`.

### 12.9 Intentar checkout con stock insuficiente (debe fallar 409)

Si añadiste 9999 unidades y haces checkout, **409 Insufficient stock**.

---

## 13. Audit Logs (solo ADMIN)

`Authorization: Bearer {{adminToken}}`. **Append-only** — no hay POST/PUT/DELETE.

### 13.1 Listar todos los logs paginados

```
GET {{baseUrl}}/api/audit-logs?page=0&size=20
```

### 13.2 Filtrar por entity

```
GET {{baseUrl}}/api/audit-logs?entity=Order&entityId=1
```

Verás la historia de tu pedido 1: CREATE, STATE_CHANGE (PENDING → PAID), STATE_CHANGE (PAID → SHIPPED), etc.

### 13.3 Filtrar por action

```
GET {{baseUrl}}/api/audit-logs?action=STATE_CHANGE
```

### 13.4 Filtrar por fecha

```
GET {{baseUrl}}/api/audit-logs?from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z
```

> El formato es ISO 8601 con Z (UTC).

### 13.5 Filtrar por actor (admin)

```
GET {{baseUrl}}/api/audit-logs?actorId=1
```

> Solo se loguean acciones de ADMIN. Si eres customer y haces cosas, NO aparecen en el log (skip silencioso).

### 13.6 Intentar acceder como customer (403)

```
GET {{baseUrl}}/api/audit-logs
Authorization: Bearer {{customerToken}}
```

**403 Access denied** (matcher en SecurityConfig).

---

## 14. Verificación del flujo completo del cliente (e2e happy path)

| Paso | Endpoint | Status esperado | Token |
|---|---|---|---|
| 1. Login cliente | `POST /api/auth/login` | 200 + token | customer |
| 2. Ver mi cart (vacío) | `GET /api/carts` | 200 + `items: []` | customer |
| 3. Añadir 2 items al cart | `POST /api/carts/items` (×2) | 200 | customer |
| 4. Ver cart con total | `GET /api/carts` | 200 + items con unitPrice + total | customer |
| 5. Checkout | `POST /api/carts/checkout` | 200 + order `PENDING` | customer |
| 6. Ver detalle del pedido | `GET /api/orders/{id}` | 200 | customer |
| 7. Pagar | `PATCH /api/orders/{id}/pay` | 200 + `status: PAID` | customer |
| 8. Verificar stock decrementado | `GET /api/products/1` | 200 con `stock: 98` (era 100) | admin |
| 9. Enviar | `PATCH /api/orders/{id}/ship` | 200 + `status: SHIPPED` | customer o admin |
| 10. Entregar | `PATCH /api/orders/{id}/deliver` | 200 + `status: DELIVERED` | customer o admin |
| 11. Ver timeline final | `GET /api/audit-logs?entity=Order&entityId={id}` | 200 con 5+ entradas | admin |
| 12. Listar pedidos final | `GET /api/orders?status=DELIVERED` | 200 con tu pedido | customer |

---

## 15. Verificación del flujo de admin (CRUD productos)

| Paso | Endpoint | Status esperado | Token |
|---|---|---|---|
| 1. Login admin | `POST /api/auth/login` | 200 + token | admin |
| 2. Crear categoría | `POST /api/categories` | 201 | admin |
| 3. Crear producto | `POST /api/products` | 201 | admin |
| 4. Actualizar producto (subir precio) | `PUT /api/products/1` | 200 + `price: 50 → 60` | admin |
| 5. Verificar audit log del cambio | `GET /api/audit-logs?entity=Product&entityId=1` | 200 con `metadata: "price: 50.00 -> 60.00; "` | admin |
| 6. Ver todos los pedidos (incluidos de customer) | `GET /api/orders` | 200 | admin |
| 7. Ver pedido de customer (admin bypass) | `GET /api/orders/1` | 200 | admin |
| 8. Borrar un pedido de customer | `DELETE /api/orders/1` | 204 | admin |
| 9. Verificar borrado | `GET /api/orders/1` | 404 | cualquiera |

---

## 16. Errores esperados (referencia rápida)

| Escenario | HTTP | Title del ProblemDetail | Notas |
|---|---|---|---|
| Login con password mal | 401 | Authentication failed | |
| Body con email no-tld | 400 | Validation error | |
| Body con `items: []` en order | 400 | Validation error | |
| Item con `productId` inexistente | 404 | Product not found | |
| `GET /api/orders/999` | 404 | Order not found | |
| `GET /api/orders/{id}` siendo de otro user (customer) | 403 | Access denied | |
| `GET /api/orders/{id}` siendo de otro user (admin) | 200 | (admin bypass) | |
| `PATCH /pay` sobre pedido ya pagado | 422 | Order invalid state | |
| `PATCH /pay` con stock insuficiente | 409 | Insufficient stock | con `productId`/`requested`/`available` en body |
| `PATCH /cancel` sobre pedido entregado | 422 | Order invalid state | |
| `PATCH /ship` sobre pedido no-PAID | 422 | Order invalid state | |
| `POST /api/orders` con `items` que piden 9999 y hay 50 | 409 | Insufficient stock | en checkout |
| `POST /api/orders` sin Authorization | 401 | (filter, no ProblemDetail) | |
| `DELETE /api/orders/1` con `customerToken` | 403 | Access denied | matcher en SecurityConfig |
| Cualquier endpoint con token inválido | 401 | (filter, no ProblemDetail) | |
| `POST /api/auth/login` 11+ veces en 1 min | 429 | (filter, no ProblemDetail) | header `Retry-After` |
| Backend caído | ECONNREFUSED | (no llega a Spring) | |

---

## 17. Tips de Postman

- **Tests tab**: añade tests que guarden los tokens automáticamente tras el login. Ejemplo para el login de admin:
  ```js
  pm.test("save adminToken", () => {
    pm.collectionVariables.set("adminToken", pm.response.json().token);
  });
  ```
  Y para el seed:
  ```js
  pm.test("save productIds", () => {
    const items = pm.response.json();
    pm.collectionVariables.set("productId1", items.productsInDb > 0 ? 1 : null);
  });
  ```
- **Collection variables** vs **Environment variables**: usa Collection si solo tienes un entorno, Environment si tienes `local` + `prod` separados.
- **Save as example** en cada response para tener fixtures rápidos.
- **Newman** (CLI de Postman) puede correr toda esta suite en CI. Exporta la collection y corre `newman run collection.json`.
- **Pre-request scripts** para generar timestamps dinámicos en los filtros de audit log.

---

## 18. Atajos para el día a día

Una vez tengas el seed y los tokens, los 4 endpoints que más vas a usar son:

```bash
# Ver todos los productos
curl -s http://localhost:8080/api/products | jq .

# Ver el cart
curl -s http://localhost:8080/api/carts -H "Authorization: Bearer $TOKEN" | jq .

# Hacer checkout rápido (asume que el cart tiene items)
curl -s -X POST http://localhost:8080/api/carts/checkout \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"shippingAddress":"Calle Mayor 1, Madrid"}' | jq .

# Pagar el último pedido
curl -s -X PATCH "http://localhost:8080/api/orders/$(curl -s http://localhost:8080/api/orders -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')/pay" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"method":"CARD"}' | jq .
```

Último comando = magia pura de bash + jq + el endpoint de la API.
