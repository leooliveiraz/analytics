#!/usr/bin/env bash
#
# Instalador de produção do Analytics.
# - Verifica Docker/Compose
# - Gera deploy/.env com segredos aleatórios (se ainda não existir)
# - Faz build e sobe a stack
#
# Uso (no servidor, dentro da pasta deploy/):
#   bash install.sh

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"
ENV_FILE="$DIR/.env"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker não encontrado. Instale: https://docs.docker.com/engine/install/" >&2
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
else
  echo "Docker Compose não encontrado." >&2
  exit 1
fi

rand() {
  local bytes="$1"
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex "$bytes"
  else
    head -c "$bytes" /dev/urandom | od -An -tx1 | tr -d ' \n'
  fi
}

if [ ! -f "$ENV_FILE" ]; then
  echo "Gerando $ENV_FILE com segredos aleatórios..."
  cat > "$ENV_FILE" <<EOF
POSTGRES_DB=analytics
POSTGRES_USER=analytics
POSTGRES_PASSWORD=$(rand 16)
POSTGRES_PORT=5432
API_PORT=8080
WEB_PORT=${WEB_PORT:-80}
JWT_SECRET=$(rand 32)
VISITOR_SALT=$(rand 16)
CORS_ALLOWED_ORIGIN_PATTERNS=*
RETENTION_EVENTS_MONTHS=${RETENTION_EVENTS_MONTHS:-144}
GEOIP_DB=/app/geoip/GeoLite2-City.mmdb
GEOIPUPDATE_ACCOUNT_ID=
GEOIPUPDATE_LICENSE_KEY=
GEOIPUPDATE_EDITION_IDS=GeoLite2-City
GEOIPUPDATE_FREQUENCY=24
EOF
  chmod 600 "$ENV_FILE"
else
  echo "Usando $ENV_FILE existente (não sobrescrevo)."
fi

mkdir -p "$DIR/geoip"

echo "Subindo a stack..."
$DC up -d --build

IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
PORT="$(grep -E '^WEB_PORT=' "$ENV_FILE" | cut -d= -f2)"
PORT="${PORT:-80}"

echo
echo "==============================================="
echo " Analytics no ar: http://${IP:-SEU_IP}:${PORT}"
echo " Crie sua conta na tela de registro."
echo "==============================================="
$DC ps
