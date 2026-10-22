#!/usr/bin/env python3
"""Siembra datos de ejemplo en el emulador local de Firestore (nunca en producción).

Requiere haber entrado en la app debug como dev@example.com y creado el hogar.
Agrega una segunda persona ficticia, ~3 meses de movimientos y presupuestos.
Uso: scripts/sembrar-dev.py [--limpiar]   (--limpiar borra movimientos y presupuestos antes)
"""
import datetime as dt
import json
import os
import random
import sys
import urllib.request

PROYECTO = os.environ.get("FIREBASE_PROJECT", "demo-finanzas")
BASE = f"http://127.0.0.1:8085/v1/projects/{PROYECTO}/databases/(default)/documents"
CABECERAS = {"Authorization": "Bearer owner", "Content-Type": "application/json"}  # owner = sin reglas


def pedir(metodo, ruta, cuerpo=None):
    req = urllib.request.Request(BASE + ruta, method=metodo, headers=CABECERAS,
                                 data=json.dumps(cuerpo).encode() if cuerpo is not None else None)
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read() or b"{}")


def valor(v):
    if isinstance(v, bool):
        return {"booleanValue": v}
    if isinstance(v, int):
        return {"integerValue": str(v)}
    if isinstance(v, dt.datetime):
        return {"timestampValue": v.strftime("%Y-%m-%dT%H:%M:%SZ")}
    if isinstance(v, list):
        return {"arrayValue": {"values": [valor(x) for x in v]}}
    if isinstance(v, dict):
        return {"mapValue": {"fields": {k: valor(x) for k, x in v.items()}}}
    return {"stringValue": v}


def campos(d):
    return {"fields": {k: valor(v) for k, v in d.items()}}


def leer(doc):
    out = {}
    for k, v in doc.get("fields", {}).items():
        (tipo, x), = v.items()
        out[k] = int(x) if tipo == "integerValue" else (
            [list(i.values())[0] for i in x.get("values", [])] if tipo == "arrayValue" else x)
    out["_id"] = doc["name"].rsplit("/", 1)[1]
    return out


def listar(ruta):
    return [leer(d) for d in pedir("GET", ruta + "?pageSize=1000").get("documents", [])]


hogares = listar("/households")
if not hogares:
    sys.exit("No hay hogar en el emulador: entra en la app como nuxapower (dev) y crea uno.")
hogar = hogares[0]
hid, yo = hogar["_id"], hogar["miembros"][0]
print(f"Hogar {hid}, miembro {yo}")

if "--limpiar" in sys.argv:
    for col in ("movimientos", "presupuestos"):
        for d in listar(f"/households/{hid}/{col}"):
            pedir("DELETE", f"/households/{hid}/{col}/{d['_id']}")
    print("Movimientos y presupuestos borrados")

# Segunda persona ficticia (la pareja), para ver filtros y gasto por persona.
pareja = "dev-cata"
if pareja not in hogar["miembros"]:
    pedir("PATCH", f"/households/{hid}?updateMask.fieldPaths=miembros", campos({"miembros": [yo, pareja]}))
pedir("PATCH", f"/households/{hid}/personas/{pareja}", campos({"nombre": "Cata (dev)", "color": 0xFFD81B60}))

cats = {c["nombre"]: c["_id"] for c in listar(f"/households/{hid}/categorias")}
gastos = {  # categoría: (veces al mes, monto mínimo, máximo, notas posibles)
    "Supermercado": (6, 8_000, 65_000, ["Líder", "Jumbo", "feria", "Unimarc", ""]),
    "Comida fuera": (4, 6_000, 38_000, ["sushi", "completos", "café", "pizza", ""]),
    "Transporte": (5, 1_000, 25_000, ["bencina", "Uber", "Bip!", ""]),
    "Cuentas": (3, 15_000, 60_000, ["luz", "agua", "internet", "gas"]),
    "Salud": (1, 5_000, 40_000, ["farmacia", "consulta"]),
    "Ocio": (2, 5_000, 30_000, ["cine", "Spotify", "libro", ""]),
    "Hogar": (1, 5_000, 80_000, ["Sodimac", "arriendo", ""]),
}
random.seed(42)
hoy = dt.date.today()
creados = 0
for meses_atras in (2, 1, 0):
    mes = (hoy.replace(day=1) - dt.timedelta(days=1) * 0)
    y, m = hoy.year, hoy.month - meses_atras
    while m <= 0:
        y, m = y - 1, m + 12
    ultimo_dia = hoy.day if meses_atras == 0 else 28
    movs = [("INGRESO", "Sueldo", 1_450_000, yo, "sueldo"), ("INGRESO", "Sueldo", 1_100_000, pareja, "sueldo")]
    for nombre, (veces, lo, hi, notas) in gastos.items():
        for _ in range(veces if meses_atras else max(1, veces * hoy.day // 30)):
            movs.append(("GASTO", nombre, random.randrange(lo, hi, 100), random.choice([yo, pareja]), random.choice(notas)))
    for tipo, cat, monto, quien, nota in movs:
        if cat not in cats:
            continue
        dia = random.randint(1, ultimo_dia) if tipo == "GASTO" else min(5, ultimo_dia)
        pedir("POST", f"/households/{hid}/movimientos", campos({
            "tipo": tipo, "monto": monto, "categoriaId": cats[cat], "personaId": quien,
            "fecha": f"{y:04d}-{m:02d}-{dia:02d}", "nota": nota, "creadoPor": quien,
            "creadoEn": dt.datetime(y, m, dia, 12, 0),
        }))
        creados += 1

# Presupuesto desde hace dos meses (los meses siguientes lo heredan).
y, m = hoy.year, hoy.month - 2
while m <= 0:
    y, m = y - 1, m + 12
montos = {"Supermercado": 250_000, "Comida fuera": 90_000, "Transporte": 60_000, "Cuentas": 120_000, "Ocio": 40_000}
pedir("PATCH", f"/households/{hid}/presupuestos/{y:04d}-{m:02d}",
      campos({"montos": {cats[k]: v for k, v in montos.items() if k in cats}}))
print(f"{creados} movimientos y presupuesto desde {y:04d}-{m:02d}")
