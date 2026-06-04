## Silvalde Web

## Contexto

Estoy desarrollando una aplicación web completa para un pequeño negocio del hogar que quiere vender sus productos online y gestionar toda la tienda desde casa. La idea es que los clientes puedan ver productos, consultar información y comprar desde casa, mientras el dueño gestiona el catálogo y los pedidos desde un panel de administración, todo adaptado a futuras mejoras y integraciones.

El proyecto se construye paso a paso, con cambios pequeños y verificables, para que sea fácil de entender, mantener y ampliar en el futuro siguiendo unas buenas prácticas tanto de programación como de ciberseguridad.

### Qué vamos a hacer
- Crear una tienda online moderna, fácil de usar, escalable y mantenible.
- Montar un panel de administración para gestionar productos, categorías, pedidos, métricas futuras funciones o mejoras.
- Preparar una base técnica limpia para crecer sin rehacerlo todo después.

### Información más detallada
Checkear el `doc.md`

### Arranque local con Docker
Desde la raíz del proyecto:

```bash
docker compose up --build
```

Servicios:
- Frontend y Nginx: `http://localhost`
- Backend: `http://localhost:8080`
- MySQL: `localhost:3306`

### Estructura
- `backend/`: API Spring Boot, configuración de base de datos y contenedorización del backend
- `frontend/`: app React servida con Nginx
- `docker-compose.yml`: orquestación completa de MySQL, backend y frontend

### Stack
- Backend: Java, Spring Boot y MySQL
- Frontend: React, Vite, JavaScript y Tailwind
- Infraestructura: Docker y Nginx

### Versiones / Roadmap
1. Roadmap de la primera versión (sujeta a cambios) para salir a producción :
- **Admin**:
- Gestión de productos.
- Gestión de categorías.
- Gestión de stock.
- Gestión de pedidos.
- Gestión de clientes.
- Dashboard con métricas.
- **Clientes**:
- Registro.
- Login.
- Gestión de perfil.
- Direcciones.
- Historial de pedidos.
- Carrito.
- Realización de pedidos.
- **Métricas para el administrador**
- Ventas totales.
- Ventas por mes.
- Ventas por categoría.
- Productos más vendidos.
- Clientes más activos.
- Ticket medio.
- Pedidos pendientes.
- Evolución de ingresos.
