# ms-rutaexpress-bff

BFF de la primera entrega. Java 21, Spring Boot 4.1.1, Maven.
Conserva artifact, group y package originales (`com.rutaexpress.ms_rutaexpress_bff`),
según la regla de no cambiarlos en README.AGENT.md. La estructura `cl.rutaexpress.bff`
del mismo documento es inconsistente con el proyecto inicial y no se aplicó.

Angular :4200 → BFF :8080 → HTTP → Shipments :8081 / Catalog :8082.
No hay persistencia ni lógica de dominio. Los otros proyectos no fueron inspeccionados.

## Dependencias y transporte

Las dependencias existentes cubren Web MVC, RestClient, Security, OAuth2 Resource Server,
Validation y pruebas. No fue necesario modificar pom.xml.
Se usa RestClient con el cliente HTTP de Java, timeouts configurables y sin redirecciones
ni reintentos automáticos (evita reenviar credenciales a otro host o duplicar escrituras).
Referencia: https://docs.spring.io/spring-framework/reference/web/webmvc-client.html

## Endpoints

Las rutas expuestas por el BFF son iguales a las rutas downstream:

| Método | Ruta BFF y downstream | Servicio | Autorización |
|---|---|---|---|
| POST | /api/shipments | Shipments | Cliente o Admin |
| GET | /api/shipments/{id} | Shipments | Admin, Operador, Cliente o Auditor |
| PUT | /api/shipments/{id}/status | Shipments | Operador o Admin |
| GET | /api/shipments?status=...&from=...&to=... | Shipments | Admin, Operador, Cliente o Auditor |
| GET | /api/catalog/services | Catalog | Cualquier JWT válido |
| POST | /api/catalog/services | Catalog | Admin |
| PUT | /api/catalog/services/{id} | Catalog | Admin |

Filtros opcionales: se envían sólo cuando están presentes, codificados como parámetros.
El formato de fechas y los valores de status los valida Shipments.
No se implementa GET /api/catalog/services/{id}: es opcional y no está confirmado.
Las rutas no autorizadas explícitamente se deniegan.

## Variables de entorno

| Variable | Default / requisito |
|---|---|
| SHIPMENTS_BASE_URL | http://localhost:8081 |
| CATALOG_BASE_URL | http://localhost:8082 |
| AZURE_ISSUER_URI | Obligatoria: issuer exacto del tenant/API |
| AZURE_AUDIENCE | Obligatoria: audience exacta que debe tener el access token del BFF |
| CORS_ALLOWED_ORIGINS | http://localhost:4200; varias separadas por coma |
| SERVER_PORT | 8080 |
| DOWNSTREAM_CONNECT_TIMEOUT | 3s |
| DOWNSTREAM_READ_TIMEOUT | 10s |

No se guardan secretos. Configurar URLs HTTPS en entornos que lo requieran.
AZURE_AUDIENCE no es una URL que el BFF consulte: es el valor esperado de `aud`.
Usar el valor emitido para la API registrada; por ejemplo `api://<API_CLIENT_ID>`
si ése es el contrato de tokens. No confundirlo con el client ID de Angular.
Los timeouts deben ser positivos. Las URLs no pueden incluir credenciales, query ni fragmento.

## JWT y roles

Spring Security funciona como OAuth2 Resource Server stateless.
Nimbus verifica firma con las claves descubiertas desde el issuer, issuer, vigencia y audience.
El descubrimiento se realiza al procesar el primer token; iniciar el contexto no requiere
contactar Entra. El issuer y la audience sí deben estar configurados.
No hay login local, sesiones, tokens de desarrollo ni bypass de seguridad.

El claim `roles: ["Admin"]` se transforma en `ROLE_Admin`.
Los nombres son sensibles a mayúsculas: Admin, Operador, Cliente, Auditor.
No se convierten scopes en roles ni se infieren permisos de dominio o pertenencia.

Cada controller recibe el Jwt ya autenticado y pasa `getTokenValue()` como argumento
local al client. Éste coloca `Authorization: Bearer <token>` exclusivamente en esa
petición. No se almacena el token en campos ni se registra en logs.
Se propaga en todas las llamadas a los servicios configurados.

**Condición de integración:** Shipments y Catalog deben aceptar ese mismo token
(issuer/audience/permisos compatibles). Si cada servicio exige su propia audience,
el reenvío directo no sirve y habrá que acordar otro flujo, por ejemplo OBO.
No se implementó intercambio de tokens porque está fuera del contrato solicitado.

CSRF está desactivado para esta API que autentica mediante Authorization, sin cookies.
CORS permite GET/POST/PUT/OPTIONS y headers Authorization/Content-Type/Accept.
Los preflight del origen permitido no requieren JWT. No se permiten comodines ni credenciales
de cookies. Se exponen ETag y Retry-After.

## DTOs y contratos pendientes

README.AGENT.md documenta métodos y rutas, pero **no publica los campos JSON** de solicitudes
ni respuestas, tipos de identificadores, enums de status o formato de listas/paginación.

ShipmentDto y CatalogServiceDto son DTOs de transporte basados en objetos JSON abiertos.
Se usan para creación/actualización y conservan campos desconocidos; no inventan campos
obligatorios, transiciones ni validaciones de negocio. Se rechaza JSON mal formado.
Las respuestas se transportan como bytes, manteniendo el cuerpo original incluso si la lista
es un array o una página. Esta decisión evita asumir un esquema externo que no está disponible.
Cuando se publiquen los esquemas, se podrán tipar los campos y añadir validaciones estructurales.
La dependencia Validation está disponible, pero no se inventaron restricciones de dominio.

Pendiente de verificar con los responsables externos:

- Existencia real de los siete endpoints documentados y sus esquemas JSON.
- Formato de status/from/to, IDs y posibles contratos de paginación.
- Compatibilidad del issuer/audience y roles de los tres servicios.
- GET de Catalog por ID, antes de exponerlo.

No se afirma que falte ningún endpoint en los otros repositorios: no fueron consultados.
Si un servicio responde 404, se devuelve ese 404; no hay datos simulados ni fallback local.

## Respuestas y errores

Se mantienen status y cuerpos downstream, incluyendo 201, 204, 400, 401, 403, 404 y 5xx.
Se propagan Content-Type, ETag, Last-Modified y Retry-After en éxito;
Content-Type y Retry-After en errores HTTP.
No se copian headers de sesión, CORS ni conexión del downstream.
Location no se publica mientras no exista un contrato para reescribir URLs externas al BFF.

Errores de conexión/timeout → 503 con ProblemDetail estable.
Otros errores de transporte y redirecciones inesperadas → 502.
Los mensajes locales no exponen URLs internas ni excepciones.
Los cuerpos de error HTTP del downstream se conservan; éste es responsable de no incluir secretos.
Errores de autenticación/autorización del BFF → 401/403 sin llamar al downstream.

## Ejecución local

Requiere JDK 21 y Maven o el wrapper incluido. En PowerShell:

```powershell
$env:AZURE_ISSUER_URI = 'https://login.microsoftonline.com/<TENANT_ID>/v2.0'
$env:AZURE_AUDIENCE = '<AUDIENCE_REAL_DE_LA_API>'
$env:SHIPMENTS_BASE_URL = 'http://localhost:8081'
$env:CATALOG_BASE_URL = 'http://localhost:8082'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:4200'
.\mvnw.cmd spring-boot:run
```

Los placeholders se sustituyen por la configuración de Entra.
Ejecutar cada servicio externo en su propio proyecto. El BFF no necesita esos proyectos
en su workspace y las pruebas no requieren servicios externos ni credenciales de Azure.

## Ejemplos de requests

Obtener un access token real para esta API con el rol correspondiente y asignarlo a
`$env:ACCESS_TOKEN` en la consola local (no guardarlo en archivos del repositorio).

```powershell
curl.exe -i 'http://localhost:8080/api/catalog/services' -H "Authorization: Bearer $env:ACCESS_TOKEN"
curl.exe -i 'http://localhost:8080/api/shipments/42' -H "Authorization: Bearer $env:ACCESS_TOKEN"
curl.exe -i 'http://localhost:8080/api/shipments?status=VALOR_DEL_CONTRATO&from=2026-09-01&to=2026-09-13' -H "Authorization: Bearer $env:ACCESS_TOKEN"
curl.exe -i -X POST 'http://localhost:8080/api/shipments' -H "Authorization: Bearer $env:ACCESS_TOKEN" -H 'Content-Type: application/json' --data-binary '@shipment.json'
curl.exe -i -X PUT 'http://localhost:8080/api/shipments/42/status' -H "Authorization: Bearer $env:ACCESS_TOKEN" -H 'Content-Type: application/json' --data-binary '@shipment-status.json'
curl.exe -i -X POST 'http://localhost:8080/api/catalog/services' -H "Authorization: Bearer $env:ACCESS_TOKEN" -H 'Content-Type: application/json' --data-binary '@catalog-service.json'
curl.exe -i -X PUT 'http://localhost:8080/api/catalog/services/7' -H "Authorization: Bearer $env:ACCESS_TOKEN" -H 'Content-Type: application/json' --data-binary '@catalog-service.json'
```

Los archivos JSON de estos ejemplos deben contener payloads válidos publicados por los servicios;
no se incluyen ejemplos de campos inventados.

## Pruebas y build

```powershell
mvn clean test
mvn clean package
# Equivalentes sin Maven en PATH:
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

Pruebas: contexto completo, filtro Bearer con decoder mockeado, 401/403, matriz de roles,
controllers, CORS, JWT validators y clientes RestClient con MockRestServiceServer.
Verifican métodos, rutas, filtros, cuerpos, tokens por request y errores HTTP/disponibilidad.
No prueban la conectividad real con Entra ni los contratos no publicados.

Verificación de esta entrega: `mvn clean test` y `mvn clean package`: BUILD SUCCESS,
42 pruebas, 0 fallos, 0 errores, 0 omitidas en ambos comandos.
El entorno tenía JAVA_HOME incorrecto y Maven fuera del PATH; se usó la distribución
Maven 3.9.16 del wrapper ajustando sólo el entorno del proceso. El JDK instalado es
26.0.2.1 y el compilador mantuvo `--release 21`. No se ejecutaron las pruebas sobre
un runtime JDK 21; esa verificación queda pendiente del entorno de entrega.
Artefacto: `target/ms-rutaexpress-bff-0.0.1-SNAPSHOT.jar`.

## Archivos de la entrega

Creados bajo `src/main/java/com/rutaexpress/ms_rutaexpress_bff/`:

- `client/DownstreamHttp.java`, `client/ShipmentsClient.java`, `client/CatalogClient.java`.
- `config/HttpClientConfig.java`.
- `controller/ShipmentBffController.java`, `controller/CatalogBffController.java`.
- `dto/ShipmentDto.java`, `dto/CatalogServiceDto.java`.
- `exception/DownstreamExceptionHandler.java`.
- `security/SecurityConfig.java`.

Creados bajo `src/test/java/com/rutaexpress/ms_rutaexpress_bff/`:

- `BffSecurityTests.java`, `client/ClientsTests.java`, `security/SecurityConfigTests.java`.

Modificados: `README.md`, `src/main/resources/application.properties` y
`src/test/java/com/rutaexpress/ms_rutaexpress_bff/MsRutaexpressBffApplicationTests.java`.
`pom.xml`, la clase de arranque y `README.AGENT.md` se conservaron sin cambios.
