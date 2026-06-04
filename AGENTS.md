# Silvalde Web

## Context
- Proyecto web para un negocio pequeño de hogar con tienda online y panel de administración.
- Stack objetivo: backend con Java, Spring Boot y MySQL; frontend con React, Vite, JavaScript y Tailwind; despliegue con Docker.

## Working Style
- Trabajar en cambios pequeños y verificables.
- Explicar las decisiones de forma clara y práctica, pensando en un nivel junior.
- Preferir el código existente y evitar refactors amplios si no son necesarios.
- Antes de cambios grandes, confirmar la dirección con el usuario.
- No introducir secretos ni credenciales en el repositorio.
- Programar pensando en futura escabilidad del proyecto y no programar para "salir del paso rápido", ni tampoco programar para que solo funcione, repito, programar pensando que implementaremos futuras funcionalidades de la tienda del hogar.

## Skills
- Usar `.agents/skills/backend-springBoot/java-spring-boot` para tareas del backend Java/Spring.
- Usar `.agents/skills/frontend-design` para UI y maquetación.
- Usar `.agents/skills/accessibility` cuando haya que mejorar accesibilidad.
- Usar `.agents/skills/seo` cuando haya que optimizar contenido público.
- Usar `.agents/skills/security` para secretos, variables de entorno, Docker seguro y hardening básico.

## Repo Notes
- `skills-lock.json` debe permanecer en la raíz del workspace.
- La carpeta `.agents/skills` es la ubicación correcta para las skills del proyecto.
- Mantener la estructura `backend/` y `frontend/` como separación principal del proyecto.
- Checkear paulatinamente el `README.md` y el `doc.md` (raíz del projecto), para actualizar contexto y ir modificando con las nuevas funcionalidades el `doc.md` sin exponer fallos de ciberseguridad.