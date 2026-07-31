---
type: port
service: aegis-identity-service
layer: domain
tags: [port, inbound, use-case]
status: implemented
port-type: inbound
---

# RegisterUserUseCase

Inbound port (interface) for user registration.

```mermaid
sequenceDiagram
    participant Ctrl as RegistrationController
    participant Port as RegisterUserUseCase (port)
    participant Svc as RegisterUserService (impl)
    participant Repo as UserRepository
    participant Hasher as PasswordHasher
    participant Event as EventPublisher

    Ctrl->>Port: register(command)
    Port->>Svc: delegate
    Svc->>Repo: findByEmail(email)
    alt email already exists
        Repo-->>Svc: found → throw DuplicateEmailException
    end
    Svc->>Hasher: hash(rawPassword)
    Hasher-->>Svc: passwordHash
    Svc->>Svc: User.create(email, passwordHash)
    Svc->>Repo: save(user)
    Svc->>Event: publish(UserRegistered)
    Svc-->>Ctrl: UserRegistrationResponse
```

## Method

```java
UserRegistrationResponse register(RegisterUserCommand command);
```

## Behavior

1. Validates email uniqueness via [[04 - Ports/outbound/UserRepository\|UserRepository]]
2. Hashes password via [[04 - Ports/outbound/PasswordHasher\|PasswordHasher]]
3. Creates [[02 - Domain Models/User\|User]] aggregate via factory method
4. Persists via [[04 - Ports/outbound/UserRepository\|UserRepository]]
5. Publishes [[03 - Domain Events/UserRegistered\|UserRegistered]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]

## Implementation

- **Implemented by**: `RegisterUserService` in [[01 - Services/Identity Service\|Identity Service]]
- **Exposed by**: `RegistrationController` (`POST /api/v1/users/register`)
