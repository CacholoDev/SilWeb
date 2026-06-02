## Silvalde Web

Silvalde Web es una tienda online para un pequeño negocio de hogar. La idea es que los clientes puedan ver productos, consultar información y comprar desde casa, mientras el dueño gestiona el catálogo y los pedidos desde un panel de administración.

El proyecto se construye paso a paso, con cambios pequeños y verificables, para que sea fácil de entender, mantener y ampliar en el futuro.

### Qué vamos a hacer
- Crear una tienda online moderna y fácil de usar.
- Montar un panel de administración para gestionar productos, categorías, pedidos y futuras funciones.
- Preparar una base técnica limpia para crecer sin rehacerlo todo después.

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

