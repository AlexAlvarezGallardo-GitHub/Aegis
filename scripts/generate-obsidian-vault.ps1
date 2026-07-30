#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Regenerates the Obsidian vault at docs/obsidian/ from the current source tree.
.DESCRIPTION
  Scans backend/, frontend/, specs/, and infra/ to rebuild notes for
  services, domain models, events, ports, infrastructure, frontend, and specs.
  Preserves the wikilink graph structure. Run after significant architecture changes.
.EXAMPLE
  ./scripts/generate-obsidian-vault.ps1
#>

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$VaultDir = Join-Path $RepoRoot "docs\obsidian"
$BackendDir = Join-Path $RepoRoot "backend"
$FrontendDir = Join-Path $RepoRoot "frontend\aegis-frontend"
$SpecsDir = Join-Path $RepoRoot "specs"
$InfraDir = Join-Path $RepoRoot "infra"

Write-Host "=== Aegis Obsidian Vault Regenerator ===" -ForegroundColor Cyan
Write-Host "Repo root : $RepoRoot"
Write-Host "Vault dir : $VaultDir"
Write-Host ""

# ─── Helpers ───────────────────────────────────────────────
function Ensure-Dir($path) { New-Item -ItemType Directory -Path $path -Force | Out-Null }

function Write-Note($relativePath, $content) {
    $full = Join-Path $VaultDir $relativePath
    $parent = Split-Path -Parent $full
    Ensure-Dir $parent
    Set-Content -Path $full -Value $content -Encoding UTF8
    Write-Host "  ✏️  $relativePath" -ForegroundColor Green
}

function Get-JavaFiles($servicePath) {
    Get-ChildItem -Path $servicePath -Filter "*.java" -Recurse -File
}

function Get-ServiceNameFromPom($pomPath) {
    $xml = [xml](Get-Content $pomPath)
    return $xml.project.artifactId
}

# ─── Clean existing vault (preserve .obsidian) ──────────────
Write-Host "Cleaning vault (preserving .obsidian config)..." -ForegroundColor Yellow
$obsidianConfig = Join-Path $VaultDir ".obsidian"
Get-ChildItem -Path $VaultDir -Directory | Where-Object { $_.Name -ne ".obsidian" } | Remove-Item -Recurse -Force
Write-Host "  Done.`n" -ForegroundColor Green

# ═══════════════════════════════════════════════════════════
# 1. SERVICES
# ═══════════════════════════════════════════════════════════
Write-Host "=== 01 - Services ===" -ForegroundColor Cyan

$serviceModules = @{}
Get-ChildItem -Path $BackendDir -Filter "pom.xml" -Recurse | ForEach-Object {
    $serviceName = Get-ServiceNameFromPom $_.FullName
    $serviceDir = $_.DirectoryName
    $serviceModules[$serviceName] = @{
        Dir = $serviceDir
        Port = $null
        HasWeb = Test-Path (Join-Path $serviceDir "src\main\java\*\web\controller")
        HasDomain = Test-Path (Join-Path $serviceDir "src\main\java\*\domain")
        HasInfra = Test-Path (Join-Path $serviceDir "src\main\java\*\infrastructure")
    }
    # Try to extract port from application.yml
    $ymlPath = Join-Path $serviceDir "src\main\resources\application.yml"
    if (Test-Path $ymlPath) {
        $yml = Get-Content $ymlPath -Raw
        if ($yml -match 'port:\s*(\d+)') { $serviceModules[$serviceName].Port = $Matches[1] }
    }
}

# Helper: count files in a package
function Count-Files($baseDir, $package) {
    $path = Join-Path $baseDir "src\main\java\*\${package}"
    if (Test-Path $path) { return (Get-ChildItem -Path $path -Filter "*.java" -File).Count }
    return 0
}

$serviceModules.Keys | ForEach-Object {
    $svc = $_
    $info = $serviceModules[$svc]
    $displayName = $svc -replace "^aegis-", ""
    $displayName = $displayName -replace "-", " "
    $displayName = (Get-Culture).TextInfo.ToTitleCase($displayName)
    $title = "$displayName Service"
    if ($svc -eq "aegis-common") { $title = "Common Module" }

    $domainModels = @()
    $domainEvents = @()
    $domainExceptions = @()
    $inboundPorts = @()
    $outboundPorts = @()
    $controllers = @()

    $domainDir = Join-Path $info.Dir "src\main\java\*\domain"
    if (Test-Path $domainDir) {
        $modelDir = Join-Path $info.Dir "src\main\java\*\domain\model"
        if (Test-Path $modelDir) { $domainModels = Get-ChildItem -Path $modelDir -Filter "*.java" -File | ForEach-Object { $_.BaseName } }

        $eventDir = Join-Path $info.Dir "src\main\java\*\domain\event"
        if (Test-Path $eventDir) { $domainEvents = Get-ChildItem -Path $eventDir -Filter "*.java" -File | ForEach-Object { $_.BaseName } }

        $exDir = Join-Path $info.Dir "src\main\java\*\domain\exception"
        if (Test-Path $exDir) { $domainExceptions = Get-ChildItem -Path $exDir -Filter "*.java" -File | ForEach-Object { $_.BaseName } }
    }

    $portInDir = Join-Path $info.Dir "src\main\java\*\domain\port\inbound"
    if (Test-Path $portInDir) { $inboundPorts = Get-ChildItem -Path $portInDir -Filter "*.java" -File | ForEach-Object { $_.BaseName } }

    $portOutDir = Join-Path $info.Dir "src\main\java\*\domain\port\outbound"
    if (Test-Path $portOutDir) { $outboundPorts = Get-ChildItem -Path $portOutDir -Filter "*.java" -File | ForEach-Object { $_.BaseName } }

    $webDir = Join-Path $info.Dir "src\main\java\*\web\controller"
    if (Test-Path $webDir) { $controllers = Get-ChildItem -Path $webDir -Filter "*.java" -File | ForEach-Object { $_.BaseName } }

    # Build note content
    $note = @"
---
type: service
service: $svc
layer: all
tags: [java, spring]
status: implemented
"@
    if ($info.Port) { $note += "`nport: $($info.Port)" }

    $note += @"

---

# $title

**Service**: `$svc`

## Layers

| Layer | Count |
|-------|-------|
"@
    if ($info.HasDomain) {
        $c = (Get-ChildItem -Path (Join-Path $info.Dir "src\main\java\*\domain") -Filter "*.java" -Recurse -File).Count
        $note += "`n| Domain | $c |"
    }
    if ($info.HasInfra) {
        $c = (Get-ChildItem -Path (Join-Path $info.Dir "src\main\java\*\infrastructure") -Filter "*.java" -Recurse -File).Count
        $note += "`n| Infrastructure | $c |"
    }
    if ($info.HasWeb) {
        $c = (Get-ChildItem -Path (Join-Path $info.Dir "src\main\java\*\web") -Filter "*.java" -Recurse -File).Count
        $note += "`n| Web | $c |"
    }

    if ($domainModels.Count -gt 0) {
        $note += "`n`n## Domain Models`n"
        $domainModels | ForEach-Object { $note += "`n- [[02 - Domain Models/$_|$_]]" }
    }

    if ($domainEvents.Count -gt 0) {
        $note += "`n`n## Domain Events`n"
        $domainEvents | ForEach-Object { $note += "`n- [[03 - Domain Events/$_|$_]]" }
    }

    if ($domainExceptions.Count -gt 0) {
        $note += "`n`n## Exceptions`n"
        $domainExceptions | ForEach-Object { $note += "`n- ``$_``" }
    }

    if ($inboundPorts.Count -gt 0) {
        $note += "`n`n## Inbound Ports`n"
        $inboundPorts | ForEach-Object { $note += "`n- [[04 - Ports/inbound/$_|$_]]" }
    }

    if ($outboundPorts.Count -gt 0) {
        $note += "`n`n## Outbound Ports`n"
        $outboundPorts | ForEach-Object { $note += "`n- [[04 - Ports/outbound/$_|$_]]" }
    }

    if ($controllers.Count -gt 0) {
        $note += "`n`n## REST Controllers`n"
        $controllers | ForEach-Object {
            $ep = ""
            if ($_ -match "Wallet") { $ep = " (wallet endpoints)" }
            elseif ($_ -match "Auth") { $ep = " (auth endpoints)" }
            elseif ($_ -match "Registration") { $ep = " (registration endpoints)" }
            $note += "`n- ``$_``$ep"
        }
    }

    # Dependencies — try to find @DependsOn or references
    $note += "`n`n## Flyway Migrations`n"
    $migrationsDir = Join-Path $info.Dir "src\main\resources\db\migration"
    if (Test-Path $migrationsDir) {
        Get-ChildItem -Path $migrationsDir -Filter "*.sql" -File | ForEach-Object { $note += "`n- ``$($_.Name)``" }
    } else {
        $note += "`n- None"
    }

    $note += "`n"
    Write-Note "01 - Services\$title.md" $note
}

# ═══════════════════════════════════════════════════════════
# 2. DOMAIN MODELS
# ═══════════════════════════════════════════════════════════
Write-Host "=== 02 - Domain Models ===" -ForegroundColor Cyan

Get-ChildItem -Path $BackendDir -Filter "pom.xml" -Recurse | ForEach-Object {
    $svcDir = $_.DirectoryName
    $modelDir = Join-Path $svcDir "src\main\java\*\domain\model"
    if (-not (Test-Path $modelDir)) { return }

    $files = Get-ChildItem -Path $modelDir -Filter "*.java" -File
    $files | ForEach-Object {
        $name = $_.BaseName
        $content = Get-Content $_.FullName -Raw
        $type = "domain-model"
        $tags = "ddd"

        if ($content -match "enum\s+$name") { $type = "value-object"; $tags = "ddd, enum" }
        elseif ($content -match "record\s+$name") { $type = "value-object" }

        # Extract fields
        $fields = @()
        if ($content -match "(?:private\s+)?(final\s+)?(\w+(?:<\w+>)?)\s+(\w+)\s*;" -and $type -eq "domain-model") {
            $fieldMatches = [regex]::Matches($content, '(?:private\s+)?(?:final\s+)?(\w+(?:<\w+>)?)\s+(\w+)\s*;')
            $fieldMatches | ForEach-Object { $fields += @{Type = $_.Groups[1].Value; Name = $_.Groups[2].Value } }
        }

        $note = @"
---
type: $type
service: $(Split-Path -Leaf $svcDir)
layer: domain
tags: [$tags]
status: implemented
---

# $name

## Source

``$((Split-Path -Leaf $svcDir))/domain/model/$name.java``

"@
        if ($fields.Count -gt 0) {
            $note += "`n## Fields`n`n| Field | Type |`n|-------|------|"
            $fields | ForEach-Object { $note += "`n| ``$($_.Name)`` | ``$($_.Type)`` |" }
        }

        $note += "`n"
        Write-Note "02 - Domain Models\$name.md" $note
    }
}

# ═══════════════════════════════════════════════════════════
# 3. DOMAIN EVENTS
# ═══════════════════════════════════════════════════════════
Write-Host "=== 03 - Domain Events ===" -ForegroundColor Cyan

Get-ChildItem -Path $BackendDir -Filter "pom.xml" -Recurse | ForEach-Object {
    $svcDir = $_.DirectoryName
    $eventDir = Join-Path $svcDir "src\main\java\*\domain\event"
    if (-not (Test-Path $eventDir)) { return }

    Get-ChildItem -Path $eventDir -Filter "*.java" -File | ForEach-Object {
        $name = $_.BaseName
        $content = Get-Content $_.FullName -Raw

        # Extract fields from record
        $fields = @()
        $recordMatch = [regex]::Match($content, "record\s+$name\(([^)]+)\)")
        if ($recordMatch.Success) {
            $recordMatch.Groups[1].Value -split "," | ForEach-Object {
                $parts = $_.Trim() -split "\s+"
                if ($parts.Count -ge 2) { $fields += @{Type = $parts[0]; Name = $parts[1] } }
            }
        }

        $topic = ""
        $svcName = Split-Path -Leaf $svcDir
        if ($name -match "(?<=[a-z])(?=[A-Z])") {
            $kebabEvent = $name -replace '(?<=[a-z])(?=[A-Z])', '-' | ForEach-Object { $_.ToLower() }
            $svcShort = $svcName -replace "aegis-", ""
            $topic = "aegis.$svcShort.$kebabEvent"
        }

        $note = @"
---
type: domain-event
service: $svcName
layer: domain
tags: [event, kafka]
status: implemented
topic: $topic
---

# $name

"@
        if ($fields.Count -gt 0) {
            $note += "`n## Schema`n`n| Field | Type |`n|-------|------|"
            $fields | ForEach-Object { $note += "`n| ``$($_.Name)`` | ``$($_.Type)`` |" }
        }

        $note += "`n`n- **Producer**: `$svcName`
- **Topic**: ``$topic``
"

        Write-Note "03 - Domain Events\$name.md" $note
    }
}

# ═══════════════════════════════════════════════════════════
# 4. PORTS
# ═══════════════════════════════════════════════════════════
Write-Host "=== 04 - Ports ===" -ForegroundColor Cyan

Get-ChildItem -Path $BackendDir -Filter "pom.xml" -Recurse | ForEach-Object {
    $svcDir = $_.DirectoryName
    $svcName = Split-Path -Leaf $svcDir

    foreach ($portType in @("inbound", "outbound")) {
        $portDir = Join-Path $svcDir "src\main\java\*\domain\port\$portType"
        if (-not (Test-Path $portDir)) { continue }

        Get-ChildItem -Path $portDir -Filter "*.java" -File | ForEach-Object {
            $name = $_.BaseName
            $content = Get-Content $_.FullName -Raw

            # Extract methods
            $methods = @()
            $methodMatches = [regex]::Matches($content, '(public\s+)?(\w+(?:<\w+>)?)\s+(\w+)\s*\(([^)]*)\)\s*;')
            $methodMatches | ForEach-Object {
                $returnType = $_.Groups[2].Value
                $methodName = $_.Groups[3].Value
                $params = $_.Groups[4].Value
                $methods += @{Return = $returnType; Name = $methodName; Params = $params }
            }

            $category = "inbound/$name"
            if ($portType -eq "outbound") { $category = "outbound/$name" }

            $note = @"
---
type: port
service: $svcName
layer: domain
tags: [port, $portType]
status: implemented
port-type: $portType
---

# $name

$portType port in the `$svcName` service.

"@
            if ($methods.Count -gt 0) {
                $note += "`n## Methods`n`n| Return | Method | Params |`n|--------|--------|--------|"
                $methods | ForEach-Object {
                    $paramsClean = if ($_.Params) { "``$($_.Params)``" } else { "—" }
                    $note += "`n| ``$($_.Return)`` | ``$($_.Name)`` | $paramsClean |"
                }
            }

            $note += "`n"
            Write-Note "04 - Ports\$category.md" $note
        }
    }
}

# ═══════════════════════════════════════════════════════════
# 5. INFRASTRUCTURE
# ═══════════════════════════════════════════════════════════
Write-Host "=== 05 - Infrastructure ===" -ForegroundColor Cyan

# Docker services
if (Test-Path $InfraDir) {
    $composeFiles = Get-ChildItem -Path $InfraDir -Filter "docker-compose*.yml" -File
    $note = @"
---
type: infrastructure
tags: [docker, devops]
status: implemented
---

# Docker Services

"@
    $composeFiles | ForEach-Object {
        $note += "- ``$($_.Name)```n"
    }

    $note += "`n## Services`n`n| Service | Image | Port |`n|---------|-------|------|"
    $composeFiles | ForEach-Object {
        $yml = Get-Content $_.FullName -Raw
        $serviceMatches = [regex]::Matches($yml, '(\w+):\s*\n\s*image:\s*(\S+)')
        $serviceMatches | ForEach-Object {
            $svcName = $_.Groups[1].Value
            $image = $_.Groups[2].Value
            $port = ""
            if ($yml -match "$svcName.*?ports:\s*\n\s*-\s*'?(\d+:\d+)'?") {
                $port = $Matches[1]
            }
            $note += "`n| $svcName | ``$image`` | $port |"
        }
    }
    $note += "`n"
    Write-Note "05 - Infrastructure\Docker Services.md" $note
}

# Flyway migrations
foreach ($svcDir in (Get-ChildItem -Path $BackendDir -Directory | Where-Object { Test-Path (Join-Path $_.FullName "src\main\resources\db\migration") })) {
    $migrationDir = Join-Path $svcDir.FullName "src\main\resources\db\migration"
    $svcName = Split-Path -Leaf $svcDir.FullName
    $migrations = Get-ChildItem -Path $migrationDir -Filter "*.sql" -File

    $note = @"
---
type: infrastructure
tags: [database, flyway, migration]
status: implemented
---

# Flyway Migrations — $svcName

"@
    $migrations | ForEach-Object { $note += "`n- ``$($_.Name)`` — $($_.BaseName)" }
    $note += "`n"
    Write-Note "05 - Infrastructure\Flyway Migrations ($svcName).md" $note
}

# ═══════════════════════════════════════════════════════════
# 6. FRONTEND
# ═══════════════════════════════════════════════════════════
Write-Host "=== 06 - Frontend ===" -ForegroundColor Cyan

if (Test-Path $FrontendDir) {
    $srcDir = Join-Path $FrontendDir "src\app"

    # Components
    $components = Get-ChildItem -Path $srcDir -Filter "*.component.ts" -Recurse -File | Where-Object { $_.Name -notlike "*.spec.*" }
    $note = @"
---
type: frontend
tags: [angular, components]
status: implemented
---

# Frontend Components

"@
    $components | ForEach-Object {
        $name = $_.BaseName
        $dir = Split-Path -Leaf (Split-Path -Parent $_.FullName)
        $note += "`n- ``$name`` ($dir)"
    }
    $note += "`n"
    Write-Note "06 - Frontend\Components.md" $note

    # Services
    $services = Get-ChildItem -Path $srcDir -Filter "*.service.ts" -Recurse -File | Where-Object { $_.Name -notlike "*.spec.*" }
    $note = @"
---
type: frontend
tags: [angular, services]
status: implemented
---

# Frontend Services

"@
    $services | ForEach-Object {
        $name = $_.BaseName
        $note += "`n- ``$name``"
    }
    $note += "`n"
    Write-Note "06 - Frontend\Services.md" $note
}

# ═══════════════════════════════════════════════════════════
# 7. SPECS
# ═══════════════════════════════════════════════════════════
Write-Host "=== 07 - Specs ===" -ForegroundColor Cyan

Get-ChildItem -Path $SpecsDir -Directory | ForEach-Object {
    $specDir = $_.FullName
    $specName = $_.Name  # e.g. "001-user-registration"
    $title = $specName -replace "^\d+-", ""
    $title = $title -replace "-", " "
    $title = (Get-Culture).TextInfo.ToTitleCase($title)
    $uc = "UC-$($specName -replace '^(\d+).*', '$1')"

    $contracts = @()
    $contractsDir = Join-Path $specDir "contracts"
    if (Test-Path $contractsDir) {
        $contracts = Get-ChildItem -Path $contractsDir -Recurse -File | ForEach-Object { $_.Name }
    }

    $specFile = Join-Path $specDir "spec.md"
    $planFile = Join-Path $specDir "plan.md"
    $tasksFile = Join-Path $specDir "tasks.md"

    $note = @"
---
type: spec
tags: [spec]
status: reviewed
uc: $uc
---

# $title

**Spec**: `$specName`

"@
    if (Test-Path $specFile) { $note += "`n- 📄 [Spec]($specName/spec.md)" }
    if (Test-Path $planFile) { $note += "`n- 📋 [Plan]($specName/plan.md)" }
    if (Test-Path $tasksFile) { $note += "`n- ✅ [Tasks]($specName/tasks.md)" }
    if ($contracts.Count -gt 0) {
        $note += "`n`n### Contracts"
        $contracts | ForEach-Object { $note += "`n- ``$_``" }
    }

    $note += "`n`
    Write-Note "07 - Specs\$title.md" $note
}

Write-Host ""
Write-Host "=== Vault regenerated successfully ===" -ForegroundColor Green
Write-Host "Location: $VaultDir"
Write-Host "Open in Obsidian: File → Open Vault → $VaultDir"
