<#
.SYNOPSIS
    Deploys the full Aegis platform locally with Docker Compose (no Kubernetes).

.DESCRIPTION
    Builds and starts the entire stack: databases, Kafka, Redis, the 7 backend
    microservices and the Angular frontend. No cluster required.

.PARAMETER Services
    Optional: only start specific services (comma separated). Defaults to all.

.PARAMETER Rebuild
    Force rebuild of images.

.PARAMETER Down
    Stop and remove all containers, volumes and networks.

.EXAMPLE
    ./deploy-local.ps1
    Starts the full stack and prints the URLs.

.EXAMPLE
    ./deploy-local.ps1 -Services "aegis-identity,aegis-wallet"
    Starts only identity and wallet (plus their dependencies).

.EXAMPLE
    ./deploy-local.ps1 -Down
    Stops everything.
#>
param(
    [string]$Services,
    [switch]$Rebuild,
    [switch]$Down
)

$ErrorActionPreference = "Stop"
$ComposeFile = Join-Path $PSScriptRoot "docker-compose.yml"

function Write-Step { param([string]$Msg) Write-Host "`n==> $Msg" -ForegroundColor Cyan }

# ── Validate docker ──────────────────────────────────────────────
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: docker is required." -ForegroundColor Red
    exit 1
}

# ── Down mode ────────────────────────────────────────────────────
if ($Down) {
    Write-Step "Stopping and removing Aegis stack"
    docker compose -f $ComposeFile down -v --remove-orphans
    Write-Host "Stack removed."
    exit 0
}

# ── Build and start ──────────────────────────────────────────────
if ($Rebuild) {
    Write-Step "Building and starting the full Aegis stack"
    if ($Services) {
        docker compose -f $ComposeFile build $($Services -split ",")
    } else {
        docker compose -f $ComposeFile build
    }
    if ($Services) {
        docker compose -f $ComposeFile up -d $($Services -split ",")
    } else {
        docker compose -f $ComposeFile up -d
    }
} else {
    Write-Step "Starting the full Aegis stack"
    if ($Services) {
        docker compose -f $ComposeFile up -d $($Services -split ",")
    } else {
        docker compose -f $ComposeFile up -d
    }
}

Write-Step "Waiting for services to become healthy (up to 180s)"
$deadline = (Get-Date).AddSeconds(180)
do {
    $unhealthy = docker compose -f $ComposeFile ps --format json | ConvertFrom-Json |
        Where-Object { $_.State -ne "running" }
    if (-not $unhealthy) { break }
    Start-Sleep -Seconds 5
} while ((Get-Date) -lt $deadline)

Write-Host ""
Write-Host "==================== AEGIS LOCAL URLS ====================" -ForegroundColor Green
Write-Host "  Frontend (Angular):        http://localhost:4200"
Write-Host "  BFF API:                   http://localhost:8082"
Write-Host "  Identity Service:          http://localhost:8081/actuator/health"
Write-Host "  Wallet Service:            http://localhost:8083/actuator/health"
Write-Host "  Reporting Service:         http://localhost:8087/actuator/health"
Write-Host "  Audit Service:             http://localhost:8088/actuator/health"
Write-Host "  Fraud Service:             http://localhost:8089/actuator/health"
Write-Host "  Kafka UI:                  http://localhost:8090"
Write-Host "  DBGate (DB management):    http://localhost:3000"
Write-Host "==========================================================" -ForegroundColor Green

Write-Host ""
Write-Host "Stream logs with:  docker compose -f $ComposeFile logs -f"
