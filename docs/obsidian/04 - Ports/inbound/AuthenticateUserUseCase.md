---
type: port
service: aegis-identity-service
layer: domain
tags: [port, inbound, use-case]
status: implemented
port-type: inbound
---

# AuthenticateUserUseCase

Inbound port (interface) for user login.

```mermaid
sequenceDiagram
    participant Ctrl as AuthController
    participant Port as AuthenticateUserUseCase (port)
    participant Svc as AuthenticateUserService (impl)
    participant Repo as UserRepository
    participant Hasher as PasswordHasher
    participant User as User (domain)
    participant Tokens as TokenProvider
    participant Event as EventPublisher

    Ctrl->>Port: authenticate(command)
    Port->>Svc: delegate
    Svc->>Svc: Email.of(command.email())
    Svc->>Repo: findByEmail(email)
    alt user not found
        Repo-->>Svc: empty → throw InvalidCredentialsException
    end
    Svc->>User: authenticate(rawPassword, hasher, correlationId)
    alt status LOCKED or SUSPENDED
        User-->>Svc: throw AccountLockedException / AccountSuspendedException
    end
    alt password invalid
        User-->>Svc: UserAuthenticated(success=false)
        Svc->>Svc: failedAttempts >= 5 → LOCKED
        Svc->>Event: publish(UserAuthenticated)
        alt locked due to failures
            Svc->>Event: publish(UserAccountLocked)
        end
        Svc-->>Ctrl: throw InvalidCredentialsException
    else success
        User-->>Svc: UserAuthenticated(success=true)
        Svc->>Event: publish(UserAuthenticated)
        Svc->>Tokens: generateAccessToken(userId, email)
        Tokens-->>Svc: accessToken
        Svc-->>Ctrl: Result(accessToken, emailVerified)
    end
```

## Method

```java
Result authenticate(Command command);
```

## Command / Result

```java
record Command(String email, String password, String correlationId) {}
record Result(String accessToken, boolean emailVerified) {}
```

## Behavior

1. Finds user by email via [[04 - Ports/outbound/UserRepository|UserRepository]]
2. Verifies password via [[04 - Ports/outbound/PasswordHasher|PasswordHasher]]
3. Tracks failed attempts (locks after 5, 15 min lockout)
4. Publishes [[03 - Domain Events/UserAuthenticated|UserAuthenticated]] on success AND failure (with `success` flag)
5. Publishes [[03 - Domain Events/UserAccountLocked|UserAccountLocked]] when the account becomes locked
6. Generates an access token via [[04 - Ports/outbound/TokenProvider|TokenProvider]]

## Implementation

- **Implemented by**: `AuthenticateUserService` in [[01 - Services/Identity Service|Identity Service]]
- **Exposed by**: `AuthController` (`POST /api/v1/auth/login`)
