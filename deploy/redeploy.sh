#!/usr/bin/env bash
#
# Atualiza o deploy a partir do repositório Git e reconstrói a stack.
#
# Uso (no servidor, dentro do repositório):
#   bash deploy/redeploy.sh

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"

cd "$ROOT"
echo "==> git pull"
git pull --ff-only

cd "$DIR"
echo "==> docker compose up -d --build"
docker compose up -d --build
echo "==> status"
docker compose ps
