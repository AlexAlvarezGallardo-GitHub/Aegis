#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Phase 2 — stands up a Kubernetes-representative load-testing sandbox on Minikube.

.DESCRIPTION
    Deploys the Aegis services into a local Minikube cluster so k6 measures the
    platform under real K8s constraints (resource limits, services, probes) that
    docker-compose cannot reproduce. The manifests live in the Aegis-GitOps repo
    (Helm charts + kustomize overlays); this script clones it and drives the
    install the same way ArgoCD does for `dev`.

    Layout used (verified against Aegis-GitOps):
      - Infra (PostgreSQL/Kafka/Redis): kustomize bases under infrastructure/<d>,
        applied via their `overlays/dev` (namespace `aegis-dev`).
      - Services: Helm charts charts/{identity,wallet,bff}, installed as releases
        `identity-dev` / `wallet-dev` / `bff-dev` (the BFF config references
        `http://identity-dev:8081` and `http://wallet-dev:8083`), with the dev
        overlay values file (-f overlays/dev/<svc>-values.yaml) and local images.
      - ServiceAccount: charts create one per release and hardcode its name in the
        Deployment template, so `serviceAccount.create` must stay true.

    Steps:
      1. minikube start (docker driver, 4 vCPU / 8 GB)
      2. Build the backend images (docker compose) and load them into the cluster
      3. Clone/update Aegis-GitOps
      4. Install infrastructure from infrastructure/<d>/overlays/dev and wait Ready
      5. Install identity-dev / wallet-dev / bff-dev with local images
      6. Wait for rollouts and port-forward the BFF to localhost:8082
      7. Print the k6 evidence commands

.PARAMETER GitOpsRepoUrl
    Aegis-GitOps clone URL. Defaults to
    https://github.com/AlexAlvarezGallardo-GitHub/Aegis-GitOps (uses
    $env:AEGIS_GITOPS_FINE_GRAINED as the token when set).

.PARAMETER GitOpsDir
    Where to clone/update Aegis-GitOps. Default: <repo>/../aegis-gitops.

.PARAMETER Namespace
    Namespace for infra + services. Must match the GitOps dev overlay. Default: aegis-dev.

.PARAMETER Cpus
.PARAMETER MemoryMb
    Minikube resources. Defaults: 4 vCPU / 8192 MB.

.PARAMETER SkipInfra
    Skip installing infrastructure (assume it already exists in the cluster).

.EXAMPLE
    ./scripts/load-sandbox-minikube.ps1

.EXAMPLE
    ./scripts/load-sandbox-minikube.ps1 -SkipInfra -GitOpsDir C:\repos\Aegis-GitOps
#>
param(
    [string]$GitOpsRepoUrl = "https://github.com/AlexAlvarezGallardo-GitHub/Aegis-GitOps",
    [string]$GitOpsDir = (Join-Path (Split-Path -Parent $PSScriptRoot) "..\aegis-gitops"),
    [string]$Namespace = "aegis-dev",
    [int]$Cpus = 4,
    [int]$MemoryMb = 8192,
    [switch]$SkipInfra
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot

function Step { param([string]$Msg) Write-Host "`n==> $Msg" -ForegroundColor Cyan }
function Fail { param([string]$Msg) Write-Host "ERROR: $Msg" -ForegroundColor Red; exit 1 }

# ── Preconditions ─────────────────────────────────────────────────────────────
foreach ($tool in @("minikube", "kubectl", "helm", "git", "docker")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { Fail "$tool is required." }
}
if ($env:AEGIS_GITOPS_FINE_GRAINED) {
    # Only used for cloning the (private) GitOps repo; ignored otherwise.
    $env:GITOPSTOKEN = $env:AEGIS_GITOPS_FINE_GRAINED
}

# ── 1. Start Minikube (sets the current kubectl context) ──────────────────────
Step "Starting Minikube (driver=docker, $Cpus vCPU / $MemoryMb MB)"
minikube start --driver=docker --cpus=$Cpus --memory=$MemoryMb
if ($LASTEXITCODE -ne 0) { Fail "minikube start failed." }
kubectl create namespace $Namespace --dry-run=client -o yaml | kubectl apply -f - | Out-Null

# ── 2. Build + load backend images (plain local names, no registry retag) ─────
$Services = @("identity", "wallet", "bff")
$Images = $Services | ForEach-Object { "infra-aegis-$_" }
$NeedBuild = @()
foreach ($img in $Images) {
    docker image inspect $img *> $null
    if ($LASTEXITCODE -ne 0) { $NeedBuild += $img }
}
if ($NeedBuild.Count -gt 0) {
    Step "Building backend images (docker compose build)"
    $composeFile = Join-Path $RepoRoot "infra\docker-compose.yml"
    docker compose -f $composeFile build aegis-identity aegis-wallet aegis-bff
}
Step "Loading images into Minikube"
foreach ($img in $Images) { minikube image load $img }

# ── 3. Clone / update Aegis-GitOps ─────────────────────────────────────────────
Step "Cloning / updating Aegis-GitOps into $GitOpsDir"
if (-not (Test-Path (Join-Path $GitOpsDir ".git"))) {
    $authUrl = $GitOpsRepoUrl
    if ($env:GITOPSTOKEN) {
        $authUrl = $GitOpsRepoUrl -replace "https://", "https://x-access-token:$($env:GITOPSTOKEN)@"
    }
    git clone --depth 1 $authUrl $GitOpsDir
} else {
    git -C $GitOpsDir pull --ff-only
}
Remove-Item Env:GITOPSTOKEN -ErrorAction SilentlyContinue

# ── 4. Infrastructure (PostgreSQL / Kafka / Redis) via dev overlays ───────────
if (-not $SkipInfra) {
    Step "Installing infrastructure from Aegis-GitOps/infrastructure/*/overlays/dev"
    foreach ($dir in @("database", "kafka", "redis")) {
        $path = Join-Path $GitOpsDir "infrastructure\$dir\overlays\dev"
        if (Test-Path (Join-Path $path "kustomization.yaml")) {
            kubectl apply -k $path | Out-Null
        } else {
            Write-Host "WARN: no dev overlay at $path — skipping $dir" -ForegroundColor Yellow
        }
    }
    Step "Waiting for infrastructure pods to be Ready (up to 240s)"
    kubectl wait --for=condition=Ready pods --all -n $Namespace --timeout=240s
    if ($LASTEXITCODE -ne 0) { Fail "Infrastructure did not become ready in time." }
}

# ── 5. Service charts (identity-dev / wallet-dev / bff-dev) with local images ──
Step "Installing service charts (identity-dev, wallet-dev, bff-dev)"
# Charts reference a ghcr pull secret in the dev overlay; with pullPolicy=Never
# it is not used, but a dummy keeps the reference valid.
kubectl create secret docker-registry ghcr-pull `
    --docker-server=ghcr.io --docker-username=DUMMY --docker-password=DUMMY `
    -n $Namespace --dry-run=client -o yaml | kubectl apply -f - | Out-Null

foreach ($svc in $Services) {
    $release = "$svc-dev"
    $chart = Join-Path $GitOpsDir "charts\$svc"
    $values = Join-Path $GitOpsDir "overlays\dev\$svc-values.yaml"
    if (-not (Test-Path (Join-Path $chart "Chart.yaml"))) {
        Write-Host "WARN: chart not found at $chart — skipping $release" -ForegroundColor Yellow
        continue
    }
    if (-not (Test-Path $values)) {
        Write-Host "WARN: overlay values not found at $values — skipping $release" -ForegroundColor Yellow
        continue
    }
    helm upgrade --install $release $chart -n $Namespace `
        -f $values `
        --set "image.repository=infra-aegis-$svc" `
        --set image.tag=latest `
        --set image.pullPolicy=Never
    if ($LASTEXITCODE -ne 0) { Fail "helm install $release failed." }
}

# ── 6. Wait + port-forward BFF ─────────────────────────────────────────────────
Step "Waiting for service rollouts"
foreach ($svc in $Services) {
    kubectl rollout status "deployment/$svc-dev" -n $Namespace --timeout=240s
    if ($LASTEXITCODE -ne 0) { Fail "Deployment $svc-dev did not roll out." }
}

$bffPort = 8082
Step "Port-forwarding BFF (svc/bff-dev) to localhost:$bffPort"
$job = Start-Job -ScriptBlock {
    param($ns, $port) kubectl port-forward -n $ns "svc/bff-dev" "$port`:$port"
} -ArgumentList $Namespace, $bffPort
Start-Sleep -Seconds 6

# ── 7. Print evidence instructions ─────────────────────────────────────────────
Write-Host ""
Write-Host "==================== AEGIS MINIKUBE SANDBOX READY ====================" -ForegroundColor Green
Write-Host "  BFF (port-forward):  http://localhost:$bffPort"
Write-Host "  Namespace:           $Namespace"
Write-Host "  Releases:            identity-dev / wallet-dev / bff-dev (pullPolicy=Never)"
Write-Host ""
Write-Host "Seed fresh user pools, then run the load evidence:"
Write-Host "  .\load\seed-users.ps1  -Prefix lguser  -Count 50"
Write-Host "  .\load\seed-users.ps1  -Prefix wltuser -Count 30"
Write-Host "  .\load\seed-users.ps1  -Prefix depuser -Count 20"
Write-Host "  .\load\seed-users.ps1  -Prefix idemuser -Count 10"
Write-Host "  .\load\run-load-tests.ps1  -BaseUrl http://localhost:$bffPort"
Write-Host "======================================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Tear down: minikube delete  |  Stop-Job -Id $($job.Id); Stop-Process -Id $($job.Id)"
