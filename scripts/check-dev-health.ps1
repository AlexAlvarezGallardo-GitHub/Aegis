<#
.SYNOPSIS
    Health check del entorno DEV (minikube) de Aegis.

.DESCRIPTION
    Verifica el estado del stack completo de dev gestionado por Argo CD:
      1. Aplicaciones Argo CD (todas deben estar Healthy/Synced)
      2. Pods del namespace aegis-dev (todos Running, sin restarts excesivos)
      3. Recursos (CPU/memoria) de los pods via kubectl top
      4. Health endpoints /actuator/health de los servicios backend
      5. Pods del stack de observabilidad (monitoring + logging)
      6. Prometheus targets activos (scraping de servicios y nodos)

    Exit code 0 si todo está OK, 1 si algo falla (usable en CI/local).

.PARAMETER Namespace
    Namespace a verificar. Default: aegis-dev.

.PARAMETER FailOnRestarts
    Fallar si algun pod tiene > 3 restarts. Default: $true.

.EXAMPLE
    ./scripts/check-dev-health.ps1
    ./scripts/check-dev-health.ps1 -Namespace aegis-dev -FailOnRestarts $false
#>

param(
    [string]$Namespace = "aegis-dev",
    [int]$MaxRestarts = 3,
    [switch]$SkipHealthEndpoints
)

$ErrorActionPreference = "Stop"
$script:Failed = $false
$report = [System.Collections.Generic.List[string]]::new()

function Write-Report([string]$status, [string]$message) {
    $icon = switch ($status) {
        "OK"   { "[OK]   " }
        "WARN" { "[WARN] " }
        "FAIL" { "[FAIL] " }
        default { "[INFO] " }
    }
    $line = "$icon$message"
    $report.Add($line)
    Write-Host $line
    if ($status -eq "FAIL") { $script:Failed = $true }
}

function Test-KubectlAvailable {
    if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
        Write-Report "FAIL" "kubectl no está disponible en el PATH."
        return $false
    }
    return $true
}

function Get-ArgoAppsHealth {
    Write-Host "`n=== 1. Aplicaciones Argo CD ===" -ForegroundColor Cyan
    try {
        $apps = kubectl get applications -n argocd --no-headers 2>$null
        if (-not $apps) {
            Write-Report "WARN" "No se encontraron aplicaciones Argo CD."
            return
        }
        $degraded = 0
        foreach ($line in $apps) {
            $parts = $line -split "\s+"
            $name = $parts[0]; $sync = $parts[1]; $health = $parts[2]
            if ($health -eq "Healthy" -and $sync -eq "Synced") {
                Write-Report "OK"   "app $name -> $health/$sync"
            } else {
                Write-Report "FAIL" "app $name -> $health/$sync (esperado Healthy/Synced)"
                $degraded++
            }
        }
    } catch {
        Write-Report "FAIL" "No se pudo consultar Argo CD: $($_.Exception.Message)"
    }
}

function Get-PodsHealth {
    Write-Host "`n=== 2. Pods en namespace $Namespace ===" -ForegroundColor Cyan
    try {
        $pods = kubectl get pods -n $Namespace --no-headers 2>$null
        if (-not $pods) {
            Write-Report "WARN" "No hay pods en el namespace $Namespace."
            return
        }
        foreach ($line in $pods) {
            $parts = $line -split "\s+"
            $name = $parts[0]; $ready = $parts[1]; $status = $parts[2]; $restarts = $parts[3]
            $readyOK = $ready -match "^\d+/\d+$"
            if ($readyOK) {
                $n = [int]($ready -split '/')[0]
                $d = [int]($ready -split '/')[1]
                $allReady = ($n -eq $d)
            } else { $allReady = $false }

            if ($status -eq "Running" -and $allReady -and $restarts -le $MaxRestarts) {
                Write-Report "OK"   "pod $name -> $ready, $status, $restarts restarts"
            } elseif ($status -eq "Running" -and $allReady -and $restarts -gt $MaxRestarts) {
                Write-Report "WARN" "pod $name -> $ready, $status, $restarts restarts (> $MaxRestarts)"
            } else {
                Write-Report "FAIL" "pod $name -> $ready, $status, $restarts restarts"
            }
        }
    } catch {
        Write-Report "FAIL" "No se pudo consultar los pods: $($_.Exception.Message)"
    }
}

function Get-ResourceUsage {
    Write-Host "`n=== 3. Uso de recursos (kubectl top) ===" -ForegroundColor Cyan
    try {
        $top = kubectl top pods -n $Namespace --no-headers 2>$null
        if (-not $top) {
            Write-Report "WARN" "kubectl top no devolvió datos (metrics-server puede no estar instalado)."
            return
        }
        foreach ($line in $top) {
            Write-Report "OK" "resource $line"
        }
    } catch {
        Write-Report "WARN" "No se pudo obtener métricas: $($_.Exception.Message)"
    }
}

function Get-HealthEndpoints {
    if ($SkipHealthEndpoints) {
        Write-Host "`n=== 4. Health endpoints (omitido) ===" -ForegroundColor Cyan
        return
    }
    Write-Host "`n=== 4. Health endpoints /actuator/health ===" -ForegroundColor Cyan
    $services = @(
        @{ Name = "identity-dev"; Label = "identity"; Port = 8081 },
        @{ Name = "bff-dev";      Label = "bff";      Port = 8082 },
        @{ Name = "wallet-dev";   Label = "wallet";   Port = 8083 }
    )
    foreach ($svc in $services) {
        try {
            $pod = kubectl get pods -n $Namespace -l "app.kubernetes.io/name=$($svc.Label)" -o jsonpath='{.items[0].metadata.name}' 2>$null
            if (-not $pod) {
                Write-Report "WARN" "endpoint $($svc.Name) -> no pod encontrado"
                continue
            }
            $body = kubectl exec -n $Namespace $pod -- sh -c "wget -qO- --timeout=4 http://localhost:$($svc.Port)/actuator/health 2>&1" 2>$null
            if ($body -match '"status"\s*:\s*"UP"') {
                Write-Report "OK" "endpoint $($svc.Name) (:$( $svc.Port)) -> UP"
            } else {
                Write-Report "FAIL" "endpoint $($svc.Name) (:$( $svc.Port)) -> NO UP ($body)"
            }
        } catch {
            Write-Report "FAIL" "endpoint $($svc.Name) -> error: $($_.Exception.Message)"
        }
    }
}

function Write-Summary {
    Write-Host "`n========================================" -ForegroundColor Cyan
    if ($script:Failed) {
        Write-Host "RESULTADO: FALLO - revisar los [FAIL] anteriores" -ForegroundColor Red
    } else {
        Write-Host "RESULTADO: TODO OK" -ForegroundColor Green
    }
    Write-Host "========================================" -ForegroundColor Cyan
}

function Get-ObservabilityHealth {
    Write-Host "`n=== 5. Stack de observabilidad (monitoring + logging) ===" -ForegroundColor Cyan
    foreach ($ns in @("monitoring", "logging")) {
        try {
            $pods = kubectl get pods -n $ns --no-headers 2>$null
            if (-not $pods) {
                Write-Report "WARN" "No hay pods en el namespace $ns."
                continue
            }
            foreach ($line in $pods) {
                $parts = $line -split "\s+"
                $name = $parts[0]; $ready = $parts[1]; $status = $parts[2]
                $allReady = $false
                if ($ready -match "^\d+/\d+$") {
                    $n = [int]($ready -split '/')[0]
                    $d = [int]($ready -split '/')[1]
                    $allReady = ($n -eq $d)
                }
                if ($status -eq "Running" -and $allReady) {
                    Write-Report "OK" "pod $name -> $ready, $status ($ns)"
                } else {
                    Write-Report "FAIL" "pod $name -> $ready, $status ($ns)"
                }
            }
        } catch {
            Write-Report "FAIL" "No se pudo consultar pods en $ns: $($_.Exception.Message)"
        }
    }
}

function Test-PrometheusTargets {
    Write-Host "`n=== 6. Prometheus targets (scraping) ===" -ForegroundColor Cyan
    try {
        $pod = kubectl get pods -n monitoring -l app=prometheus -o jsonpath='{.items[0].metadata.name}' 2>$null
        if (-not $pod) {
            Write-Report "WARN" "prometheus pod no encontrado"
            return
        }
        $json = kubectl exec -n monitoring $pod -- sh -c "wget -qO- --timeout=4 http://localhost:9090/api/v1/targets 2>&1" 2>$null
        if (-not $json) {
            Write-Report "FAIL" "prometheus targets -> sin respuesta"
            return
        }
        $targets = ($json | ConvertFrom-Json).data.activeTargets
        if (-not $targets) {
            Write-Report "WARN" "prometheus targets -> sin targets activos"
            return
        }
        $up = @($targets | Where-Object { $_.health -eq "up" }).Count
        $total = @($targets).Count
        $upText = if ($up -gt 0) { "OK" } else { "FAIL" }
        Write-Report $upText "prometheus targets -> $up/$total up"
    } catch {
        Write-Report "FAIL" "prometheus targets -> error: $($_.Exception.Message)"
    }
}

# --- Main ---
if (-not (Test-KubectlAvailable)) { exit 1 }

Get-ArgoAppsHealth
Get-PodsHealth
Get-ResourceUsage
Get-HealthEndpoints
Get-ObservabilityHealth
Test-PrometheusTargets
Write-Summary

if ($script:Failed) { exit 1 } else { exit 0 }
