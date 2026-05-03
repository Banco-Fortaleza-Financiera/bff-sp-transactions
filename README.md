# bff-sp-transactions

Proyecto Java 21 con Gradle Groovy, Spring Boot 3.x, Spring Data JPA, Redis Cache, OpenAPI Generator, MapStruct, JUnit 5 y JaCoCo.

## Como ejecutar

```sh
./gradlew bootRun
```

## Como compilar

```sh
./gradlew clean build
```

## OpenAPI

La fuente de verdad del contrato esta en la raiz del proyecto:

```text
bff-openapi-sp-transaction.yaml
```

El codigo generado queda en `build/generated/src/main/java`.

## Arquitectura

- `controller`: expone los endpoints generados desde OpenAPI.
- `services`: orquesta los casos de uso segun `operationId`.
- `repository`: encapsula acceso JPA.
- `repository/*/entity`: entidades necesarias segun el contrato y DBML.
- `services/mapper`: conversion entre entidades y modelos OpenAPI.
- `security`: validacion transversal del header `x-userid`.
- `handler`: manejo estandar de errores.
