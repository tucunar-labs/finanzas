#!/usr/bin/env bash
# Publica el APK release de Finanzas en el repositorio F-Droid "Nuxapower Apps" (~/code/nuxapower-apps).
# Uso: scripts/publicar-apk.sh [ruta.apk]   (por defecto, el APK release recién compilado; no compila)
set -euo pipefail
cd "$(dirname "$0")/.."
exec "$HOME/code/nuxapower-apps/publicar.sh" "$(realpath "${1:-app/build/outputs/apk/release/app-release.apk}")"
