#!/usr/bin/env bash
# Desarrollo local sin Google ni Firebase reales:
#   1. emuladores de Firebase (Auth :9099, Firestore :8085, UI http://127.0.0.1:4000) con las mismas
#      firestore.rules; los datos persisten en .emulador/ entre sesiones,
#   2. emulador Android (AVD pixel_36, script ~/bin/emu),
#   3. compila el debug una vez, lo instala y lo abre. En la app: "Entrar como nuxapower@gmail.com (dev)".
# Uso: scripts/dev.sh [--sin-compilar]     Detener emuladores de Firebase: scripts/dev.sh --parar
set -euo pipefail
cd "$(dirname "$0")/.."

ADB="$HOME/Android/Sdk/platform-tools/adb"
LOG=/tmp/finanzas-firebase-emuladores.log

if [ "${1:-}" = "--parar" ]; then
  pkill -INT -f "firebase-tools.*emulators:start" && echo "Emuladores de Firebase detenidos (datos guardados en .emulador/)" || echo "No estaban corriendo"
  exit 0
fi

# 1. Emuladores de Firebase (necesitan Java 21+; el JDK por defecto del sistema es 17).
if ! curl -s -o /dev/null http://127.0.0.1:9099; then
  echo "Levantando emuladores de Firebase…"
  mkdir -p .emulador
  JAVA_HOME=/usr/lib/jvm/java-26-openjdk PATH="/usr/lib/jvm/java-26-openjdk/bin:$PATH" \
    setsid nohup npx -y firebase-tools@15 emulators:start --only auth,firestore \
      --project finanzas-hogar-b2ac --import .emulador --export-on-exit .emulador >"$LOG" 2>&1 < /dev/null &
  for _ in $(seq 1 90); do
    curl -s -o /dev/null http://127.0.0.1:9099 && curl -s -o /dev/null http://127.0.0.1:8085 && break
    sleep 2
  done
  curl -s -o /dev/null http://127.0.0.1:8085 || { echo "No arrancaron; mira $LOG" >&2; exit 1; }
fi
echo "Firebase local listo (UI: http://127.0.0.1:4000)"

# 2. Emulador Android.
"$HOME/bin/emu"

# 3. App debug (una sola compilación).
[ "${1:-}" = "--sin-compilar" ] || ./gradlew :app:assembleDebug --console=plain -q
APK=app/build/outputs/apk/debug/app-debug.apk
# Si hay una release instalada (otra firma), se desinstala: es el emulador, no hay datos que perder.
if ! "$ADB" -s emulator-5554 install -r "$APK" 2>&1 | tee /dev/stderr | grep -q Success; then
  "$ADB" -s emulator-5554 uninstall cl.nuxapower.finanzas >/dev/null
  "$ADB" -s emulator-5554 install "$APK" | tail -1
fi
"$ADB" -s emulator-5554 shell am start -n cl.nuxapower.finanzas/.MainActivity >/dev/null
echo "Abierta en emulator-5554"
