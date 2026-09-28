#!/usr/bin/env bash
#
# Baixa/atualiza o banco GeoLite2 da MaxMind para deploy/geoip/.
# Requer GEOIPUPDATE_ACCOUNT_ID e GEOIPUPDATE_LICENSE_KEY em deploy/.env.
#
# Uso (no WSL):
#   bash deploy/geoip/update.sh

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$DIR/../.env"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
fi

if [ -z "${GEOIPUPDATE_ACCOUNT_ID:-}" ] || [ -z "${GEOIPUPDATE_LICENSE_KEY:-}" ]; then
  echo "Defina GEOIPUPDATE_ACCOUNT_ID e GEOIPUPDATE_LICENSE_KEY em $ENV_FILE" >&2
  exit 1
fi

EDITION="${GEOIPUPDATE_EDITION_IDS:-GeoLite2-City}"

echo "Baixando $EDITION para $DIR ..."
docker run --rm \
  -v "$DIR:/usr/share/GeoIP" \
  -e "GEOIPUPDATE_ACCOUNT_ID=$GEOIPUPDATE_ACCOUNT_ID" \
  -e "GEOIPUPDATE_LICENSE_KEY=$GEOIPUPDATE_LICENSE_KEY" \
  -e "GEOIPUPDATE_EDITION_IDS=$EDITION" \
  maxmindinc/geoipupdate

echo "Concluido. Arquivos em $DIR:"
ls -la "$DIR"
echo
echo "Se estiver usando a base Country, ajuste GEOIP_DB=/app/geoip/GeoLite2-Country.mmdb no deploy/.env"
