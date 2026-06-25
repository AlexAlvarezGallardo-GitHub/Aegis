---
name: scaffold-service
description: Use when creating a new Spring Boot microservice. Generates the complete hexagonal architecture scaffold including Maven module, package structure, domain model skeleton, application services, infrastructure adapters, REST controllers, and test stubs.
---

# Scaffold Service

Generate a complete Spring Boot microservice following Aegis hexagonal architecture and DDD conventions.

## Input

The user provides:
- Service name (e.g., `identity`, `wallet`, `payment`)
- Brief description of the service's bounded context
- Key domain entities (optional)

## Generation Steps

### 1. Maven Module

Create `aegis-<service>-service/pom.xml`:
- Parent: `com.aegis:aegis-parent`
- Spring Boot starters: web, data-jpa, security, actuator, validation
- Kafka: spring-kafka
- Database: postgresql driver
- Test: junit-jupiter, mockito-core, testcontainers, wiremock

### 2. Package Structure

```
src/main/java/com/aegis/<service>/
├── domain/
│   ├── model/
│   ├── event/
│   ├── exception/
│   └── port/
│       ├── inbound/
│       └── outbound/
├── application/
│   ├── service/
│   ├── mapper/
│   └── dto/
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── adapter/
│   ├── messaging/
│   ├── config/
│   └── security/
└── web/
    ├── controller/
    ├── advice/
    └── filter/
```

### 3. Domain Layer

Generate:
- Base domain entity (if provided)
- Domain event base class
- Service-specific exception extending `AegisException`
- Inbound port interface (use case)
- Outbound port interfaces (repository, gateway)

### 4. Application Layer

Generate:
- Use case implementation service
- DTO records
- Mapper interface (MapStruct or manual)

### 5. Infrastructure Layer

Generate:
- JPA entity mapping
- Spring Data repository
- Repository adapter implementing outbound port
- Kafka producer/consumer stubs
- Spring configuration class
- Security configuration

### 6. Web Layer

Generate:
- REST controller with CRUD endpoints
- Exception handler (`@RestControllerAdvice`)
- Request filter stub

### 7. Configuration

Generate:
- `application.yml` with service config
- `application-dev.yml` for local development
- Kafka topic definitions

### 8. Tests

Generate:
- Unit test stub for service
- Integration test stub with Testcontainers
- `docker-compose.yml` for local dependencies

## Conventions

All naming, API, Kafka, database, and error response conventions are defined in `.specify/memory/constitution.md` §Technology Stack & Infrastructure Constraints.
