<#
.SYNOPSIS
    Seeds a fresh pool of k6 load-test users into the Identity service.

.DESCRIPTION
    Each k6 scenario should use its own fresh pool so the domain rule
    "max 5 wallets per user" (WALLET_LIMIT_EXCEEDED) never skews results.
    Register a pool per scenario, e.g.:
      ./seed-users.ps1 -Prefix wltuser -Count 30 -BaseUrl http://localhost:8081

.PARAMETER Prefix
    User email prefix: <prefix><n>@aegis.test

.PARAMETER Count
    How many users to register (default 60).

.PARAMETER BaseUrl
    Identity service base URL (default http://localhost:8081).

.PARAMETER Password
    Password for all seeded users (default LoadTest123!).
#>
param(
    [Parameter(Mandatory = $true)][string]$Prefix,
    [int]$Count = 60,
    [string]$BaseUrl = "http://localhost:8081",
    [string]$Password = "LoadTest123!"
)

$ErrorActionPreference = "Stop"
$ok = 0; $dup = 0; $fail = 0

foreach ($i in 1..$Count) {
    $email = "${Prefix}${i}@aegis.test"
    $body = @{
        email     = $email
        password  = $Password
        firstName = "Load"
        lastName  = "User${i}"
    } | ConvertTo-Json -Compress

    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/api/v1/users/register" -Method Post `
            -ContentType "application/json" -Body $body -TimeoutSec 15
        if ($r.StatusCode -eq 201) { $ok++ } else { $fail++ }
    }
    catch {
        # 409 on a leftover user from a previous run is fine
        $dup++
    }
}

Write-Host "Seeded '$Prefix' pool: registered=$ok duplicates=$dup failures=$fail" -ForegroundColor Green
Write-Host "Use with k6: -e USER_PREFIX=$Prefix" -ForegroundColor Green
