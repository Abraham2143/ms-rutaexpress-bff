# README.AGENT.md — ms-rutaexpress-bff

## Contexto importante

Este repositorio contiene SOLO `ms-rutaexpress-bff`.

`ms-rutaexpress-shipments`, `ms-rutaexpress-catalog` y Angular están en repositorios/proyectos separados y pueden estar abiertos en otras ventanas de IntelliJ.

Por lo tanto:

- No asumir acceso al código de otros repositorios.
- No intentar modificar Shipments, Catalog o Angular.
- Consumir servicios externos únicamente por HTTP.
- Trabajar con contratos documentados.
- Usar URLs configurables mediante variables de entorno.
- Si un endpoint esperado no existe en otro servicio, informar la incompatibilidad; no inventar datos ni modificar el otro repo.

## Proyecto

```text
Microservicio: ms-rutaexpress-bff
Group: cl.rutaexpress
Package base: cl.rutaexpress.bff
Java: 21
Maven
Puerto: 8080
```

## Alcance de la primera entrega

Implementar:

- Spring Web
- HTTP Client
- Spring Security
- OAuth2 Resource Server
- Validation
- JWT Azure AD
- roles
- clientes HTTP a Shipments y Catalog
- propagación de Bearer token
- Controllers BFF
- DTOs
- CORS
- manejo de errores downstream
- pruebas básicas

NO implementar:

- JPA
- Oracle
- Repository
- Entity
- RabbitMQ
- Kafka
- Zookeeper
- Docker
- AWS
- persistencia propia
- lógica de negocio perteneciente a los microservicios de dominio

## Arquitectura local

Cada aplicación se ejecuta por separado:

```text
Angular :4200
    |
    v
BFF :8080
    |
    +--> Shipments :8081   [otro repo/app]
    |
    +--> Catalog :8082     [otro repo/app]
```

El BFF no necesita que los otros repositorios estén dentro del mismo proyecto IntelliJ. Solo necesita que sus aplicaciones estén ejecutándose y sean accesibles por URL.

## URLs configurables

```properties
rutaexpress.shipments.base-url=${SHIPMENTS_BASE_URL:http://localhost:8081}
rutaexpress.catalog.base-url=${CATALOG_BASE_URL:http://localhost:8082}
```

Nunca hardcodear URLs productivas.

## Responsabilidad

El BFF:

- recibe requests de Angular;
- valida JWT;
- aplica autorización;
- reenvía requests;
- propaga JWT;
- adapta DTOs si hace falta;
- maneja errores downstream.

No debe:

- acceder a Oracle;
- guardar entidades;
- crear repositories;
- decidir transiciones de Shipment;
- modificar capacidad por lógica propia;
- duplicar lógica de dominio.

Regla:

```text
BFF coordina.
Shipments y Catalog deciden reglas de negocio.
```

## Estructura sugerida

```text
cl.rutaexpress.bff
├── config
├── security
├── controller
├── client
├── dto
├── exception
└── BffApplication
```

Sugerencia:

```text
client/
├── ShipmentsClient.java
└── CatalogClient.java

controller/
├── ShipmentBffController.java
└── CatalogBffController.java
```

No crear `entity/` ni `repository/`.

## Contrato esperado con Shipments

El BFF puede asumir estos endpoints porque están definidos por la pauta:

```http
POST /api/shipments
GET /api/shipments/{id}
PUT /api/shipments/{id}/status
GET /api/shipments?status=...&from=...&to=...
```

## Contrato esperado con Catalog

```http
GET /api/catalog/services
POST /api/catalog/services
PUT /api/catalog/services/{id}
```

Si el repo de Catalog también implementa:

```http
GET /api/catalog/services/{id}
```

el BFF puede soportarlo.

Si no existe, no inventarlo como si existiera.

## Cliente HTTP

Usar la solución disponible en el proyecto, preferiblemente `RestClient` si es compatible.

No acoplar código a `localhost`; usar las properties.

Cada client debe poder probarse/mockearse independientemente.

## Propagación JWT

Angular enviará:

```http
Authorization: Bearer <access_token>
```

El BFF debe:

1. validar el token;
2. aplicar roles;
3. reenviar el Bearer token a Shipments/Catalog si están protegidos.

Flujo:

```text
Angular -> Bearer JWT -> BFF -> Bearer JWT -> servicio
```

No:

- guardar tokens globalmente;
- imprimir tokens;
- inventar tokens;
- eliminar seguridad para simplificar pruebas finales.

## Seguridad

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${AZURE_ISSUER_URI}
```

Roles:

```text
Admin
Operador
Cliente
Auditor
```

Si Azure entrega:

```json
{"roles":["Admin"]}
```

mapear a authorities Spring como:

```text
ROLE_Admin
```

Validar audience `api://<API_CLIENT_ID>` si la configuración actual lo requiere.

## Autorización sugerida

Catalog:

- GET -> autenticado según política.
- POST -> Admin.
- PUT -> Admin.

Shipments:

- POST -> Cliente/Admin.
- PUT status -> Operador/Admin.
- GET -> según rol.
- Auditor -> lectura cuando corresponda.

No inventar reglas multi-tenant/dominio no definidas.

## CORS

Permitir desarrollo Angular:

```properties
rutaexpress.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:4200}
```

No dejar wildcard inseguro como solución final.

## DTOs

Crear DTOs locales del BFF.

No copiar entities de otros microservicios.

Mantener contratos compatibles con los JSON esperados.

## Errores downstream

Preservar status útiles:

```text
400 downstream -> 400
403 downstream -> 403
404 downstream -> 404
servicio caído -> 502/503
```

No convertir todo en 500.

Usar `@RestControllerAdvice` o mecanismo equivalente.

## application.properties base

```properties
spring.application.name=ms-rutaexpress-bff
server.port=8080

rutaexpress.shipments.base-url=${SHIPMENTS_BASE_URL:http://localhost:8081}
rutaexpress.catalog.base-url=${CATALOG_BASE_URL:http://localhost:8082}

spring.security.oauth2.resourceserver.jwt.issuer-uri=${AZURE_ISSUER_URI}

rutaexpress.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:4200}
```

No agregar datasource.

## Pruebas mínimas

- 401 sin JWT;
- 403 sin rol;
- request autorizado;
- llamada correcta a Shipments;
- llamada correcta a Catalog;
- propagación Bearer;
- 404 downstream;
- servicio downstream no disponible.

Los otros microservicios no deben necesitar estar levantados para todas las pruebas unitarias.

## Reglas para el agente

1. Trabajar SOLO en este repositorio.
2. No intentar abrir/modificar otros repositorios.
3. Revisar primero `pom.xml` y properties.
4. No agregar JPA/Oracle.
5. No crear Entity/Repository.
6. No implementar RabbitMQ/Kafka.
7. No hardcodear URLs productivas.
8. No duplicar lógica de dominio.
9. Mantener JWT activo.
10. Propagar token por request.
11. No inventar respuestas si falta un servicio.
12. Si el contrato real de un downstream difiere, informar el problema.
13. Mantener controllers delgados.
14. Mantener el proyecto compilable.
15. No cambiar Java 21, Maven, artifact o package.

## Orden de trabajo

1. Revisar proyecto.
2. Configurar URLs downstream.
3. Crear DTOs.
4. Crear ShipmentsClient.
5. Crear CatalogClient.
6. Crear Controllers.
7. Manejar errores downstream.
8. Configurar JWT/roles.
9. Configurar propagación del token.
10. Configurar CORS.
11. Agregar pruebas.
12. Ejecutar:

```bash
mvn clean test
mvn clean package
```

## Cómo probar integración local

Los repositorios siguen separados.

Ejecutar cada app en su propia ventana:

```text
Catalog   -> :8082
Shipments -> :8081
BFF       -> :8080
Angular   -> :4200
```

El BFF solo necesita conectividad HTTP a `8081` y `8082`.

## Al finalizar

Informar:

- archivos creados/modificados;
- endpoints BFF;
- contratos downstream utilizados;
- variables de entorno;
- mecanismo de propagación JWT;
- resultado de tests/build;
- cualquier endpoint faltante o incompatible en Shipments/Catalog.
