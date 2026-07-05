# Tasks: UC-003 BFF

## Phase 1: Spec & Infrastructure
- [ ] Add Redis service to docker-compose.yml
- [ ] Add aegis-bff-service module to root pom.xml
- [ ] Create aegis-bff-service pom.xml

## Phase 2: BFF Core
- [ ] Create BffApplication.java
- [ ] Create SessionJwtStore.java (store/retrieve JWT from HttpSession)
- [ ] Create BffService.java (WebClient proxy to Identity Service)
- [ ] Create BffAuthController.java (login, logout, me, refresh)

## Phase 3: Security
- [ ] Create SecurityConfig.java (CSRF, session, permit auth endpoints)
- [ ] Create JwtProxyFilter.java (attach JWT to outgoing requests)

## Phase 4: Config
- [ ] application.yml (port 8082, Redis, identity service URL)
- [ ] application-dev.yml

## Phase 5: Frontend
- [ ] Update AuthService (remove localStorage, use /api/bff/auth/*)
- [ ] Update proxy.conf.json (point to BFF :8082)
- [ ] Update environment.ts (apiUrl = '' → BFF proxy)

## Phase 6: Tests
- [ ] BffAuthControllerTest
- [ ] Frontend tests pass
- [ ] Full build
