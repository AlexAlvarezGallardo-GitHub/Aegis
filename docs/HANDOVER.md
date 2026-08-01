# Session Handover — 2026-08-02

Estado y plan para retomar el trabajo. Ver visión completa en [docs/PLATFORM_VISION.md](PLATFORM_VISION.md).

## Estado actual

### Sprints implementados y mergeados
| Sprint | Issue | PRs | Estado |
|--------|-------|-----|--------|
| 1 — CI Optimization | #84 | #92, #94, #96 | ✅ merged |
| 2 — GitOps Foundation | #85 | #93, #95 | ✅ merged |
| 3 — Helm Charts and Overlays | #86 | #97 | ✅ merged |
| 4 — Argo CD and Promotion | #87 | #101 (docs) | 🔶 PR abierto |
| Obsidian fixes / dark mode | — | #98, #99, #100 | ✅ merged |

### Repos
- **`Aegis`**: código + workflows + vault Obsidian.
- **`Aegis-GitOps`**: charts Helm, overlays (dev/pre/stage/prod), applications Argo CD por entorno, app-of-apps, bootstrap argocd (`infrastructure/argocd/`), `scripts/setup-minikube.ps1`, README con guía local.

### Herramientas instaladas (hoy)
- JDK 21 (Temurin 21.0.12) ✅
- Helm v4.2.3 (winget) ✅
- Argo CD CLI v3.4.5 (winget) ✅
- Headlamp (app de escritorio) ✅
- k3d v5.9.0 instalado pero **no funciona** en Docker Desktop (faltan módulos kernel `br_netfilter`/`iptable_nat`) → **se sustituye por minikube**
- minikube (K8s 1.30.0) arranca OK

### Docker compose (app local sin K8s) — ✅ funciona
- `infra/deploy-local.ps1` crea el stack completo (17 contenedores).
- Fix aplicado: `kafka-ui` tag `v0.7.5` → `latest` (el tag no existía).
- URLs: frontend `http://localhost:4200`, BFF `:8082`, identity `:8081`, wallet `:8083`, reporting `:8087`, audit `:8088`, fraud `:8089`, Kafka UI `:8090`, DBGate `:3000`.
- **Pendiente de commit**: `infra/deploy-local.ps1` (nuevo) + `infra/docker-compose.yml` (fix kafka-ui). Están en la rama local `feature/084-deploy-local-script`.

## Problemas / bloqueos

1. **minikube saturado con docker compose**: si docker compose está arriba, el API server de minikube da `TLS handshake timeout`. → Hay que **parar docker compose** antes de usar minikube (o aumentar recursos).
2. **k3d no usable** en este Docker Desktop → usar minikube.
3. **Argo CD en minikube**: `kubectl apply` del install upstream fue OK pero el `rollout status` falló por el timeout de recursos. Hay que verificar pods (`kubectl get pods -n argocd`) y terminar el setup.
4. **PR #101** (docs Argo CD vault) abierto → mergear.
5. **Falta conectar Argo CD** al repo privado `Aegis-GitOps` (necesita `argocd repo add` con PAT) y aplicar `applications/aegis-app-of-apps.yaml`.
6. **imagePullSecrets**: ya configurado en los overlays (`ghcr-pull`). Falta crear el secret `docker-registry` por namespace en el clúster (con PAT `read:packages`).

## Plan para mañana

### 1. Cerrar pendientes de hoy
- [ ] Commitear `infra/deploy-local.ps1` + fix `docker-compose.yml` (rama `feature/084-deploy-local-script`) y abrir PR.
- [ ] Mergear PR #101.

### 2. Terminar entorno local K8s (minikube)
- [ ] `docker compose -f infra/docker-compose.yml down` (liberar recursos).
- [ ] `minikube start --cpus 4 --memory 8192` y verificar `kubectl get nodes`.
- [ ] Verificar pods Argo CD: `kubectl get pods -n argocd`; si el install quedó incompleto, re-aplicar `kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/v2.12.3/manifests/install.yaml`.
- [ ] Crear secret `ghcr-pull` por namespace (`aegis-dev` al menos) con PAT `read:packages`.
- [ ] `argocd login localhost:8080 --insecure` (port-forward) con admin + password del secret.
- [ ] `argocd repo add https://github.com/AlexAlvarezGallardo-GitHub/Aegis-GitOps --username <user> --password <PAT>`.
- [ ] Aplicar App of Apps: `kubectl -n argocd apply -f <Aegis-GitOps>/applications/aegis-app-of-apps.yaml` (o solo `applications/dev/`).
- [ ] Verificar sync: `argocd app list`, `kubectl get pods -n aegis-dev`.
- [ ] Configurar Headlamp (kubeconfig de minikube) y k9s.

### 3. Continuar roadmap
- [ ] **Sprint 5 — Observability** (#88): Prometheus, Grafana, Loki, Tempo, OpenTelemetry (reemplazar stubs de `infrastructure/monitoring` y `infrastructure/logging`).
- [ ] Sprint 6 — Security (Cosign signing, Dependabot/Renovate).
- [ ] Sprint 7 — Reporting y notificaciones.
- [ ] Sprint 8 — Entornos efímeros y DORA.

## Comandos útiles

```powershell
# App local (sin K8s)
docker compose -f infra/docker-compose.yml up -d --build
.\infra\deploy-local.ps1 -Down

# K8s local
minikube start --cpus 4 --memory 8192
kubectl config use-context minikube
kubectl -n argocd get pods
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | % { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) }
kubectl -n argocd port-forward svc/argocd-server 8080:443
argocd login localhost:8080 --insecure
kubectl port-forward svc/argocd-server -n argocd 8080:443
```
