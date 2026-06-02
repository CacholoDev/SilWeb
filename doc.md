# Silvalde Web - Technical Notes

Este documento sirve como referencia técnica interna. Aquí se explica la configuración del proyecto, el bloque de seguridad y cómo encaja todo para que luego sea más fácil ampliar el sistema.

## 1. Estructura general

- `backend/`: API Spring Boot.
- `frontend/`: interfaz React con Vite.
- `docker-compose.yml`: levanta MySQL, backend y frontend.
- `.env`: variables locales sensibles, no se sube al repositorio.
- `.env.example`: plantilla segura de las variables necesarias.

## 2. Variables de entorno

La idea es no escribir secretos en el código ni en el compose. El flujo es este:

1. El valor real vive en `.env`.
2. `docker-compose.yml` lo lee desde el entorno.
3. Spring Boot lo consume con `@Value` en `application.properties` o directamente en clases de configuración.

Variables actuales importantes:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_CORS_ALLOWED_ORIGIN`
- `JWT_SECRET`
- `JWT_EXPIRATION_MINUTES`
- `JWT_ISSUER`
- `APP_ADMIN_USERNAME`
- `APP_ADMIN_PASSWORD`
- `APP_ADMIN_ROLES`
- `APP_CUSTOMER_USERNAME`
- `APP_CUSTOMER_PASSWORD`
- `APP_CUSTOMER_ROLES`

## 3. Docker

### 3.1 `docker-compose.yml`

El compose orquesta tres servicios:

- `mysql`: base de datos MySQL 8.
- `backend`: API Spring Boot.
- `frontend`: app React servida por Nginx.

El backend se conecta a MySQL mediante el nombre del servicio (`mysql`) dentro de la red de Docker. Eso evita usar `localhost` dentro del contenedor, que sería incorrecto porque `localhost` ahí apunta al propio contenedor y no a la base de datos.

### 3.2 `backend/Dockerfile`

Usa una construcción en dos fases:

- fase `build`: compila el proyecto con Maven;
- fase `runtime`: ejecuta el `.jar` con una imagen JRE ligera.

Eso reduce el tamaño final y evita meter herramientas de compilación dentro de la imagen de producción.

### 3.3 `frontend/Dockerfile` y `frontend/nginx.conf`

El frontend se construye con Node y luego Nginx sirve el resultado compilado.

Nginx también actúa como proxy para `/api`, redirigiendo esas peticiones al backend. Eso permite que el frontend use la misma URL base en producción y que el navegador no tenga problemas de CORS en el dominio final.

## 4. Spring Boot backend

### 4.1 `application.properties`

La configuración principal lee valores desde variables de entorno.

Ejemplos:

- `spring.datasource.url=${SPRING_DATASOURCE_URL:...}`
- `spring.datasource.username=${SPRING_DATASOURCE_USERNAME:...}`
- `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:...}`
- `app.cors.allowed-origin=${APP_CORS_ALLOWED_ORIGIN:...}`
- `app.security.jwt.secret=${JWT_SECRET:...}`

La parte importante es el formato `${VARIABLE:valor_por_defecto}`:

- si la variable existe, se usa el valor real;
- si no existe, se usa el fallback;
- esto ayuda en local, pero sin meter secretos en el código.

### 4.2 `SecurityConfig.java`

Esta clase centraliza Spring Security.

Responsabilidades:

- define el `SecurityFilterChain`;
- desactiva CSRF para esta API JWT;
- deja la sesión en modo `STATELESS`;
- habilita CORS;
- permite `POST /api/auth/login` sin autenticación;
- protege el resto de rutas;
- registra el filtro JWT antes del filtro de autenticación estándar.

También crea:

- `AuthenticationManager`: valida login y password;
- `UserDetailsService`: usuarios en memoria por ahora;
- `PasswordEncoder`: BCrypt;
- `CorsConfigurationSource`: controla los orígenes permitidos.

La parte de usuarios en memoria está pensada como paso intermedio. Más adelante se puede cambiar por usuarios reales en MySQL sin tocar toda la arquitectura JWT.

### 4.3 `JwtService.java`

Esta clase se encarga de trabajar con el token.

Hace cuatro cosas:

- genera JWT cuando el login es correcto;
- incluye `username` y `roles` como claims;
- valida firma y expiración;
- extrae usuario y roles del token.

Conceptos clave:

- `issuer`: identifica quién emite el token;
- `expirationMinutes`: tiempo de vida del token;
- `signingKey`: clave con la que se firma el token;
- `claims`: datos que viajan dentro del JWT.

La firma se deriva a partir de `JWT_SECRET`, usando SHA-256 para obtener una clave válida para HMAC.

### 4.4 `JwtAuthenticationFilter.java`

Este filtro se ejecuta en cada request.

Flujo:

1. Lee el header `Authorization`.
2. Comprueba si empieza por `Bearer `.
3. Extrae el token.
4. Valida el token con `JwtService`.
5. Carga el usuario con `UserDetailsService`.
6. Si todo es correcto, guarda la autenticación en `SecurityContextHolder`.

Esto permite que luego Spring vea al usuario como autenticado durante toda la request.

### 4.5 `AuthController.java`

Este controller expone el login:

- recibe `username` y `password`;
- usa `AuthenticationManager` para validar credenciales;
- genera el token con `JwtService`;
- devuelve un `AuthResponse` con token, usuario y roles.

### 4.6 DTOs

`LoginRequest.java` y `AuthResponse.java` son DTOs simples:

- `LoginRequest`: entrada del login;
- `AuthResponse`: salida del login.

Se usan records para mantener el código corto e inmutable.

## 5. Seguridad actual

Medidas aplicadas ahora mismo:

- secretos fuera del código fuente;
- `.env` ignorado por git;
- JWT firmado;
- API stateless;
- CORS controlado;
- contraseñas con BCrypt;
- roles desde el login.

## 6. Próximo paso lógico

Cuando toque pasar de demo técnica a aplicación real, el siguiente cambio debería ser:

- usuarios en MySQL en vez de en memoria;
- tablas de roles y permisos;
- registro de usuarios;
- refresh tokens si hace falta;
- endpoints protegidos por roles (`ADMIN`, `CUSTOMER`).