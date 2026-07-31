#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Activates the Aegis git hooks for local development.
.DESCRIPTION
  Configures git to use the .githooks/ directory for hook scripts.
  This enforces branch naming, commit message format, and secret scanning
  on every local commit.
.EXAMPLE
  ./scripts/setup-hooks.ps1
#>

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

Write-Host "=== Aegis Git Hooks Setup ===" -ForegroundColor Cyan
Write-Host "Repo root: $RepoRoot"

$hooksPath = Join-Path $RepoRoot ".githooks"

if (-not (Test-Path $hooksPath)) {
    Write-Host "ERROR: .githooks/ directory not found at $hooksPath" -ForegroundColor Red
    exit 1
}

git -C $RepoRoot config core.hooksPath .githooks

$currentConfig = git -C $RepoRoot config --get core.hooksPath
Write-Host ""
Write-Host "Git hooks activated: core.hooksPath = $currentConfig" -ForegroundColor Green
Write-Host ""
Write-Host "Active hooks:" -ForegroundColor Yellow
Get-ChildItem -Path $hooksPath -File | ForEach-Object {
    Write-Host "  - $($_.Name)" -ForegroundColor White
}
Write-Host ""
Write-Host "Hooks enforce:" -ForegroundColor Yellow
Write-Host "  pre-commit  : branch naming convention + secret scan in staged diff"
Write-Host "  commit-msg  : conventional commit format (type(scope): description)"
Write-Host ""
Write-Host "Done." -ForegroundColor Green
