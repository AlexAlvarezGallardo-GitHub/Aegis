#!/bin/bash
set -euo pipefail

echo "=== Building Java modules ==="
cd backend
# NOTE: Tests are intentionally skipped here for faster local iteration.
# CI (GitHub Actions) runs the full test suite on every PR.
# To build WITH tests locally, run: mvn clean install
mvn clean install -DskipTests

echo ""
echo "=== Building Docker images ==="
cd ..

docker compose -f infra/docker-compose.yml build

echo ""
echo "=== Starting all services ==="
docker compose -f infra/docker-compose.yml up -d
