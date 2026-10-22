#!/usr/bin/env bash
# Publica firestore.rules en el proyecto Firebase (sin firebase CLI, vía API de Rules).
set -euo pipefail
P="${FIREBASE_PROJECT:?export FIREBASE_PROJECT before running}"
CUENTA="${GCLOUD_ACCOUNT:-$(gcloud config get-value account 2>/dev/null)}"
cd "$(dirname "$0")/.."
T=$(gcloud auth print-access-token --account="$CUENTA")
API="https://firebaserules.googleapis.com/v1/projects/${P}"
BODY=$(python3 -c 'import json;print(json.dumps({"source":{"files":[{"name":"firestore.rules","content":open("firestore.rules").read()}]}}))')
RULESET=$(curl -sf -X POST -H "Authorization: Bearer $T" -H "x-goog-user-project: ${P}" -H "Content-Type: application/json" \
  "${API}/rulesets" -d "$BODY" | python3 -c 'import json,sys;print(json.load(sys.stdin)["name"])')
curl -sf -X PATCH -H "Authorization: Bearer $T" -H "x-goog-user-project: ${P}" -H "Content-Type: application/json" \
  "${API}/releases/cloud.firestore" -d "{\"release\":{\"name\":\"projects/${P}/releases/cloud.firestore\",\"rulesetName\":\"${RULESET}\"}}" >/dev/null
echo "Reglas publicadas: ${RULESET}"
