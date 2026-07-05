@echo off
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
