#!/bin/bash
set -euo pipefail

echo "=== Building Java modules ==="
cd backend
mvn clean install -DskipTests

echo ""
echo "=== Building Docker images ==="
cd ..

docker compose -f infra/docker-compose.yml build

echo ""
echo "=== Starting all services ==="
docker compose -f infra/docker-compose.yml up -d
