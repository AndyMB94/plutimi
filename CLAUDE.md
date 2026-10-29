# Plutimi

E-commerce de petshop + veterinaria. Proyecto de portafolio para demostrar buenas prácticas en Java y Spring Boot.

**Documentación de referencia.** Consulta solo las secciones relevantes para la tarea actual; no leas los documentos completos en cada sesión.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): cómo y por qué (capas, reglas entre módulos, errores, auth, patrones, glosario).
- [docs/PLAN.md](docs/PLAN.md): módulos, modelo de datos, endpoints y tareas por fase con checkboxes. Marca `[x]` las tareas al completarlas.

## Sobre mí
- Desarrollador backend con experiencia en Python/Django, aprendiendo Java y Spring Boot.
- Explícame los conceptos de Spring cuando aparezcan por primera vez, comparándolos con Django si ayuda.
- Prefiero entender el código antes que recibirlo hecho: cuando la tarea sea grande, propón primero un plan.
- Los comandos de terminal los ejecuto yo: dime cuál, desde qué carpeta y qué hace.

## Stack
- Backend: Java 21, Spring Boot 4.1, Maven, Spring Data JPA, PostgreSQL 17, Flyway, Spring Security (JWT), Lombok, Bean Validation, springdoc-openapi
- Frontend (más adelante): Angular + TypeScript
- Infraestructura: Docker Compose, Nginx, despliegue en VPS (plutimi.andymallcco.dev)

## Estructura
```
plutimi/
├── backend/    Spring Boot (paquete base: dev.andymallcco.plutimi)
├── frontend/   Angular (pendiente)
├── docs/       ARCHITECTURE.md y PLAN.md
├── docker-compose.yml   PostgreSQL para desarrollo (puerto 5434 en el host; el 5433 lo usa otro proyecto en WSL)
└── CLAUDE.md
```

## Entorno local (Windows)
- JDK 21 (Microsoft OpenJDK) en C:\Users\LENOVO\.jdks\ms-21.0.13.0.2, configurado como JAVA_HOME.
- Compilar desde la terminal en backend/: `.\mvnw.cmd compile`
- IntelliJ usa Bundled Maven (el sync con el wrapper fallaba).
- Docker Desktop instalado; la app se ejecuta desde IntelliJ, Docker solo para la base de datos por ahora.
- Puertos ocupados por otros proyectos en Ubuntu/WSL: 5433, 6380, 8100 y 80. No usarlos para Plutimi.

## Convenciones (resumen; el detalle está en ARCHITECTURE.md)

**Idioma**
- Todo el código en inglés: paquetes, clases, tablas, columnas y endpoints.
- Textos que ve el usuario final (errores, validaciones, correos) en español.
- Documentación en español.

**Arquitectura**
- Monolito modular por funcionalidad: `shared`, `auth`, `user`, `species`, `catalog`, `cart`, `order`, `payment`, `checkout`, `pet`, `medicalrecord`, `appointment`, `notification`.
- Cada módulo: entity, repository (package-private), service, controller, mapper y `dto/`. Lo transversal en `shared` (config, exception, persistence, security, money, text, web).
- Entre módulos: solo a través del service del otro módulo; relaciones por ID, no por asociación JPA; efectos secundarios con eventos de Spring.
- Servicios externos (pagos, correo) en clases client/gateway propias.

**Dominio y DTOs**
- Modelo rico: la entidad JPA es también la entidad de dominio y contiene sus reglas (`order.cancel()`).
- Value Objects con `@Embeddable` (`Money`, `Address`). `BigDecimal` para dinero, nunca `double`.
- Las entidades nunca salen del service. La API expone solo `record`s: `CreateXRequest`, `UpdateXRequest`, `XResponse`, `XSummaryResponse`, `XFilter`. Mappers manuales.
- Prohibido "model" en nombres de clases y paquetes.
- Entidades: `@Getter`, `@Setter` solo donde no haya reglas, `@NoArgsConstructor(access = PROTECTED)`. Nunca `@Data`.

**Base de datos**
- Database first: primero la migración Flyway y después la entidad. `ddl-auto=validate`.
- Migraciones `V<n>__<verb>_<object>.sql` (ej. `V1__create_categories_table.sql`). Nunca modificar una migración ya aplicada.
- Tablas en plural y `snake_case`; `BIGINT IDENTITY`; `TIMESTAMPTZ` ↔ `Instant`. Tablas de unión N:M con PK compuesta.
- Enum (`VARCHAR` + `@Enumerated(STRING)`) solo si cada valor tiene comportamiento en el código (estados, Strategy); si son datos gestionables, tabla de referencia.
- Seeders: datos de referencia en migraciones `V<n>__insert_...` (todos los entornos); datos demo en migraciones repetibles e idempotentes `db/demo/R__demo_<nn>_<tema>.sql` (solo perfil `dev`).

**API REST**
- `/api/v1/<recurso-en-plural>` en kebab-case. Acciones de negocio: `POST /api/v1/orders/{id}/cancel`.
- 201 (+ `Location`) al crear, 204 al eliminar, 400 validación, 401/403 seguridad, 404 no existe o es ajeno, 409 conflicto, 422 regla de negocio.
- `@Valid` + Bean Validation. Paginación en todos los listados con `PageResponse<T>`.
- Documentada con springdoc-openapi.

**Errores**
- `shared/exception/GlobalExceptionHandler` (`@RestControllerAdvice`) devuelve `ProblemDetail`.
- Bases: `ResourceNotFoundException` (404), `ConflictException` (409), `BusinessException` (422). Las excepciones de cada módulo heredan de ellas.
- Los services lanzan; los controllers no usan try/catch. Los 500 no exponen detalles y se registran en el log.

**Seguridad**
- Spring Security con JWT: access token de 15 min + refresh token rotativo (cookie HttpOnly, guardado hasheado).
- RBAC en base de datos: `roles`, `permissions`, `role_permissions`; un rol por usuario (`users.role_id`). Roles base `CUSTOMER`, `VET`, `ADMIN`.
- Los endpoints comprueban permisos con `@PreAuthorize("hasAuthority('<recurso>:<acción>')")`, nunca `hasRole`. La propiedad ("lo mío") la aplica el service; los permisos `*:read-all` amplían el alcance.
- JWT solo dentro del módulo `auth`. El resto usa `CurrentUserProvider` / `AuthenticatedUser` de `shared/security`.

**Principios**
- Inyección por constructor (`@RequiredArgsConstructor`); nunca `@Autowired` en atributos.
- Interfaces solo con varias implementaciones reales o como frontera de arquitectura; sin `ServiceImpl` por defecto.
- Patrones previstos: Strategy (pagos), State (estados de pedidos y citas), Observer (eventos), Specification (filtros del catálogo), Facade (checkout).
- Perfiles `dev` y `prod`; secretos siempre en variables de entorno.
- Tests en cada fase, junto al código que prueban.

**Git**
- Commits en español siguiendo Conventional Commits: `<tipo>(<ámbito>): <descripción>`.
  - Tipo en inglés: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `build`, `ci`.
  - Ámbito: el nombre del módulo cuando aplique (`catalog`, `auth`, `shared`...).
  - Descripción breve en español, en imperativo (ej. `feat(catalog): agregar búsqueda de productos con filtros`).
  - Cuerpo detallado en español explicando qué se hizo y por qué.
- Una rama por sección del plan (ej. `feature/1.1-base-proyecto`), que se integra a `main` con un Pull Request en GitHub.

## Fases (detalle y checkboxes en docs/PLAN.md)
1. Base (errores, configuración, Swagger), catálogo y usuarios con JWT
2. Carrito, pedidos y pago en modo prueba
3. Veterinaria: mascotas, historial clínico y citas
4. Extras: notificaciones, calidad, CI y despliegue

Fase actual: 1
