# Implementation Plan: UC-003 BFF

**Branch**: `feature/uc-003-bff` | **Date**: 2026-07-05

## Implementation Order

```
1. docker-compose.yml        ← Add Redis
2. Root pom.xml              ← Add bff module
3. aegis-bff-service/        ← New module
   ├── pom.xml
   ├── BffApplication.java
   ├── SessionJwtStore.java
   ├── BffAuthController.java
   ├── BffService.java
   ├── SecurityConfig.java
   ├── JwtProxyFilter.java
   ├── application.yml
   ├── application-dev.yml
   └── Test files
4. Frontend                  ← Update AuthService, proxy, env
```

## Key Decisions

| Decision | Choice |
|----------|--------|
| Session store | Redis (distributed, multi-instance BFF) |
| HTTP client for proxy | WebClient (reactive, non-blocking) |
| BFF port | 8082 |
| CSRF | Enabled with cookie-based auth |
| Session cookie | HttpOnly, SameSite=Strict, Secure in prod |
| Token storage in session | In-memory HttpSession (backed by Redis) |
