#!/usr/bin/env bash
#
# Baixa/atualiza a base GeoIP gratuita do DB-IP (formato MaxMind .mmdb),
# sem precisar de conta/credenciais.
#
# Uso:
#   bash deploy/geoip/update-dbip.sh            # mês atual
#   bash deploy/geoip/update-dbip.sh 2026-09    # mês específico
#
# Depois aponte no deploy/.env:
#   GEOIP_DB=/app/geoip/dbip-city-lite.mmdb
#
# A API recarrega o banco sozinha (verifica a cada hora) ou reinicie: docker compose up -d api.

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONTH="${1:-$(date -u +%Y-%m)}"
URL="https://download.db-ip.com/free/dbip-city-lite-${MONTH}.mmdb.gz"
OUT="$DIR/dbip-city-lite.mmdb"

echo "Baixando ${URL} ..."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
curl -fsSL "$URL" -o "$tmp/dbip.mmdb.gz"
gzip -dc "$tmp/dbip.mmdb.gz" > "$OUT"
chmod 644 "$OUT" 2>/dev/null || true

echo "OK: $OUT"
ls -lh "$OUT"
echo
echo "Garanta no deploy/.env: GEOIP_DB=/app/geoip/dbip-city-lite.mmdb"
