#!/usr/bin/env bash
# Publica un APK ya compilado en el repositorio F-Droid privado (Neo Store lo ofrece como actualización).
# Uso: scripts/publicar-apk.sh [ruta.apk]   (por defecto, el APK release recién compilado)
# No compila: primero ./gradlew :app:assembleRelease.
set -euo pipefail
cd "$(dirname "$0")/.."

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
MANTENER=5                                  # versiones que quedan en el repo; las más viejas se borran
AAPT2="$HOME/Android/Sdk/build-tools/36.0.0/aapt2"
RUTA=$(cat fdroid/.ruta_secreta)            # segmento secreto de la URL (no versionado)

[ -f "$APK" ] || { echo "No existe $APK" >&2; exit 1; }
BADGING=$("$AAPT2" dump badging "$APK")
PAQUETE=$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<<"$BADGING")
CODIGO=$(sed -n "s/.*versionCode='\([0-9]*\)'.*/\1/p" <<<"$BADGING")
NOMBRE=$(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" <<<"$BADGING")
echo "Publicando $PAQUETE $NOMBRE (versionCode $CODIGO)"

cp "$APK" "fdroid/repo/${PAQUETE}_${CODIGO}.apk"

# Deja solo las $MANTENER versiones más nuevas.
ls fdroid/repo/"${PAQUETE}"_*.apk | sort -t_ -k2 -n | head -n -"$MANTENER" | xargs -r rm -v

(cd fdroid && ANDROID_HOME="$HOME/Android/Sdk" fdroid update 2>&1 | grep -E "WARNING|ERROR|Finished" | grep -v repo_icon)

set -a; . "$HOME/.config/vps/credentials.env"; set +a
# Sin rsync en el VPS: se sube a repo.nuevo y se reemplaza de una vez (nunca queda un repo a medias).
tar -C fdroid/repo -czf - . | ssh -i "$VPS_SSH_KEY" "$VPS_USER@$VPS_HOST" "set -e
  D=/srv/fdroid/$RUTA; mkdir -p \$D; rm -rf \$D/repo.nuevo; mkdir \$D/repo.nuevo
  tar -C \$D/repo.nuevo -xzf -
  rm -rf \$D/repo.viejo; [ -d \$D/repo ] && mv \$D/repo \$D/repo.viejo; mv \$D/repo.nuevo \$D/repo; rm -rf \$D/repo.viejo"

URL="https://apps.nuxapower.cl/$RUTA/repo"
for i in 1 2 4 8 16; do   # la primera petición puede dar 525 mientras Caddy emite el certificado
  CODIGO_HTTP=$(curl -s -o /dev/null -w '%{http_code}' "$URL/index-v1.jar") && [ "$CODIGO_HTTP" = 200 ] && break
  sleep "$i"
done
echo "index-v1.jar -> HTTP $CODIGO_HTTP"
echo "Listo: $URL"
