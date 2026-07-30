@echo off
setlocal

if /I "%1"=="dev" goto dev_mode

REM ── Production mode ──────────────────────────────────────
echo === Building Java modules ===
cd backend
call mvn clean install -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%

echo.
echo === Building Docker images ===
cd ..
docker compose -f infra/docker-compose.yml build
if %errorlevel% neq 0 exit /b %errorlevel%

echo.
echo === Starting all services ===
docker compose -f infra/docker-compose.yml up -d
exit /b %errorlevel%

REM ── Development mode (hot reload) ────────────────────────
:dev_mode
echo === Starting development environment ===
echo (infrastructure from docker-compose.yml, apps from docker-compose.dev.yml)
echo.
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d --remove-orphans
if %errorlevel% neq 0 exit /b %errorlevel%

echo.
echo === Development environment started ===
echo Frontend : http://localhost:4200
echo Identity  : http://localhost:8081
echo BFF       : http://localhost:8082
echo Wallet    : http://localhost:8083
echo.
echo Code changes are detected automatically.
echo Stop with: docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml down
