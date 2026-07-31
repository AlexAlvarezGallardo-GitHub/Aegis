# Common Module

**Purpose**: Shared library used by all Aegis microservices. Contains cross-cutting concerns and shared abstractions.

## Contents

| Class | Purpose |
|-------|---------|
| `UuidV7Generator` | UUIDv7 generation (time-ordered, DB-friendly identifiers) |
| `AegisException` | Base class for all domain exceptions with `code` and `message` |

## Usage

Add as a dependency in any service's `pom.xml`:

```xml
<dependency>
    <groupId>com.aegis</groupId>
    <artifactId>aegis-common</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Conventions

- Domain exceptions MUST extend `AegisException`
- Identifiers should use `UuidV7Generator.generate()` for time-ordered UUIDs
