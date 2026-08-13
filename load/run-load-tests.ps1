<#
.SYNOPSIS
    Runs the k6 load-test scenarios against a sandbox and stores evidence.

.DESCRIPTION
    Executes login, wallets, deposits, idempotency and transfers against the Aegis
    BFF/service APIs using the grafana/k6 Docker image, and writes both the text
    summary and a k6 summary-export JSON per scenario into evidence/load/.

    Each scenario uses its own fresh user pool (see seed-users.ps1) so the
    domain rule "max 5 wallets per user" does not skew the results.

.PARAMETER BaseUrl
    BFF URL reachable from inside the k6 container. Default uses
    host.docker.internal (Docker Desktop) pointing at the host's BFF.

.PARAMETER EvidenceDir
    Where to write the evidence files (default evidence/load).
#>
param(
    [string]$BaseUrl = "http://host.docker.internal:8082",
    [string]$EvidenceDir = "evidence/load"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$loadAbs = Join-Path $repoRoot "load"
$evAbs = Join-Path $repoRoot $EvidenceDir
New-Item -ItemType Directory -Force -Path $evAbs | Out-Null

# Scenario => user pool
$scenarios = @(
    @{ Name = "login";      Pool = "lguser";   }
    @{ Name = "wallets";    Pool = "wltuser";  }
    @{ Name = "deposits";   Pool = "depuser";  }
    @{ Name = "idempotency"; Pool = "idemuser" }
    @{ Name = "transfers";  Pool = "trfuser";  }
    @{ Name = "payments";   Pool = "payuser";  }
)

foreach ($s in $scenarios) {
    $name = $s.Name
    Write-Host "`n==> Running scenario: $name (pool: $($s.Pool))" -ForegroundColor Cyan
    docker run --rm `
        -v "${loadAbs}:/scripts" `
        -v "${evAbs}:/out" `
        grafana/k6:latest run `
        --summary-export "/out/$name-summary.json" `
        "/scripts/k6/$name.js" `
        -e "BASE_URL=$BaseUrl" `
        -e "USER_PREFIX=$($s.Pool)" 2>&1 |
        Tee-Object -FilePath (Join-Path $evAbs "$name.txt") |
        Out-Null
    Write-Host "    -> evidence/load/$name.txt + $name-summary.json"
}

Write-Host "`nDone. Evidence written to $evAbs"
