# Finanzas

App Android privada para llevar gastos e ingresos de una pareja (máximo 2 personas por hogar).
Kotlin + Jetpack Compose (Material 3), MVVM, Hilt, Firebase Auth (Google) + Cloud Firestore con caché offline.

## Funciones

- **Ingreso rápido** (pantalla inicial): monto con teclado propio, Gasto/Ingreso, categoría, persona
  (por defecto quien usa el teléfono), fecha (hoy por defecto) y nota. Monto + categoría + Guardar = 3 toques.
  Snackbar con *Deshacer*. Extras: tecla `+` para sumar varias boletas, mantener ⌫ para limpiar,
  notas sugeridas según la categoría.
- **Widget de inicio "Gasto rápido"**: grilla con las categorías de gasto; tocar una abre una hoja con el
  teclado y esa categoría elegida. Persona = quien usa el teléfono, fecha = hoy. Funciona sin conexión.
- **Presupuesto**: monto mensual por categoría de gasto, con barra de avance (verde < 80 %, ámbar hasta
  100 %, rojo y ⚠ al pasarse) y total del mes. Un mes sin presupuesto propio hereda el del último mes
  definido; editar guarda solo para el mes mostrado. Tocar una categoría abre sus movimientos.
- **Movimientos**: agrupados por día con neto diario, totales del mes, filtros por mes, tipo, categoría y
  persona, búsqueda en todos los meses (nota, categoría, persona, monto). Tocar = editar (y duplicar con
  fecha de hoy), deslizar a la izquierda = eliminar con *Deshacer*. Ícono de nube tachada en lo que aún no
  se sincroniza.
- **Resumen**: ingresos, gastos y balance del mes con variación contra el mes anterior; gasto o ingreso
  por categoría (barras con %), gasto por persona, tendencia de 6 meses. Tocar una barra abre Movimientos
  filtrado.
- **Categorías** (desde Ajustes): crear, editar nombre/emoji/color, archivar/restaurar, reordenar.
  Las archivadas no aparecen al ingresar, pero siguen en historial, filtros y reportes.
- **Ajustes**: miembros, código de invitación (con botón compartir), editar nombre y color propios,
  exportar CSV (todo o un rango de fechas) con el selector de compartir.
- **Atajos del launcher**: mantener presionado el ícono → *Nuevo gasto* / *Nuevo ingreso*.
- Offline: todo se guarda local y sincroniza solo. Cambios del otro miembro llegan en tiempo real.
- Tema claro/oscuro del sistema (colores dinámicos en Android 12+).

## Estructura

```
app/src/main/java/cl/nuxapower/finanzas/
├── FinanzasApp.kt, MainActivity.kt      # Hilt app; raíz decide login / hogar / app
├── di/FirebaseModule.kt                 # FirebaseAuth, Firestore (caché persistente), scope de app
├── data/model/                          # Persona, Categoria, Movimiento, Hogar, mapeo a Firestore, semillas
├── data/repo/
│   ├── AuthRepository, SesionRepository # login y users/{uid}.householdId → estado de sesión
│   ├── HogarRepository                  # crear hogar / unirse con código / personas
│   ├── HogarDatos                       # listeners en tiempo real compartidos (hogar, personas, categorías, movimientos)
│   ├── MovimientoRepository, CategoriaRepository
│   └── Solicitudes                      # pedidos entre pantallas (atajos, filtro desde Resumen)
├── domain/                              # Resumen, Presupuesto y CSV (lógica pura, con tests)
├── ui/                                  # login, hogar, ingreso, movimientos, presupuesto, resumen, categorias, ajustes, comun, theme
├── widget/                              # widget Glance, hoja de gasto rápido, caché de categorías para el widget
└── util/                                # formato CLP, fechas, código de invitación, Flow de Firestore
firestore.rules                          # reglas de seguridad
scripts/publicar-reglas.sh               # publica las reglas sin firebase CLI
```

Dependencias y versiones: `gradle/libs.versions.toml` (AGP 9.4, Kotlin 2.4, Compose BOM 2026.09,
Firebase BOM 34.19, Hilt 2.60, Credential Manager 1.6). `compileSdk` 37, `targetSdk` 36, `minSdk` 26.

## Datos en Firestore

```
users/{uid}                          { householdId }
invites/{codigo}                     { householdId }          # se lee por código, no se puede listar
households/{hid}                     { codigoInvitacion, miembros[≤2], creadoPor, creadoEn }
households/{hid}/personas/{uid}      { nombre, color }
households/{hid}/categorias/{id}     { nombre, icono, color, tipo, archivada, orden }
households/{hid}/movimientos/{id}    { tipo, monto, categoriaId, personaId, fecha "yyyy-MM-dd", nota, creadoPor, creadoEn }
households/{hid}/presupuestos/{mes}  { montos: { categoriaId: monto } }       # mes = "yyyy-MM"
```

## Puesta en marcha (desde cero)

El proyecto actual ya está creado: `finanzas-hogar-b2ac` (cuenta alonso@tucunar.com). Para repetirlo:

1. **Proyecto**: en <https://console.firebase.google.com> → *Agregar proyecto* (o agregar Firebase a un
   proyecto de Google Cloud existente). Analytics no hace falta. Plan Spark (gratis) alcanza.
2. **Firestore**: *Firestore Database → Crear base de datos*, modo producción, región `southamerica-west1`.
3. **Reglas**: pegar `firestore.rules` en *Firestore → Reglas → Publicar*, o `scripts/publicar-reglas.sh`
   (usa gcloud; variables `FIREBASE_PROJECT` y `GCLOUD_ACCOUNT`).
4. **Google Sign-In**: *Authentication → Comenzar → Sign-in method → Google → Habilitar*, elegir correo de soporte.
5. **App Android**: *Configuración del proyecto → Agregar app → Android*, paquete `cl.nuxapower.finanzas`.
   Agregar las huellas SHA-1 de debug y release:
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -storepass android -alias androiddebugkey | grep SHA1
   keytool -list -v -keystore keystore/release.jks -alias finanzas | grep SHA1
   ```
6. **google-services.json**: descargarlo y dejarlo en `app/google-services.json` (no se versiona).
   Si se agregan huellas después, hay que volver a descargarlo.

## Desarrollo local (sin Google ni Firebase reales)

```
scripts/dev.sh                # emuladores de Firebase + emulador Android + compila e instala el debug
scripts/dev.sh --sin-compilar # reinstala el último debug compilado
scripts/dev.sh --parar        # detiene los emuladores de Firebase (los datos quedan en .emulador/)
```

- El build **debug** se conecta a los emuladores de Firebase (Auth :9099, Firestore :8085) en
  `10.0.2.2` (la máquina host vista desde el emulador Android) y la pantalla de login muestra
  **"Entrar como nuxapower@gmail.com (dev)"**: el emulador de Auth acepta un ID token de Google falso,
  así el flujo es el mismo que en producción pero sin cuenta real. Los datos no tocan producción.
- Los emuladores cargan las mismas `firestore.rules` (sirven para probarlas). UI: http://127.0.0.1:4000.
- Teléfono físico en la LAN: `./gradlew assembleDebug -Pfinanzas.emulador=<ip-del-pc>`. Para que el debug
  use Firebase real: `-Pfinanzas.emulador=` (vacío).
- El build **release** no cambia: `EMULADOR_HOST` es `""` y el botón dev no existe.
- Requiere Java 21+ para los emuladores (el script usa `/usr/lib/jvm/java-26-openjdk`) y `npx`.
  El emulador Android es el AVD `pixel_36` (`~/bin/emu`).

## Compilar e instalar

Requisitos: JDK 17+, Android SDK con platform 37 (`sdk.dir` en `local.properties`).

```
# Clave de firma release (una sola vez; guárdala: sin ella no se puede actualizar la app instalada)
keytool -genkeypair -keystore keystore/release.jks -alias finanzas -keyalg RSA -keysize 4096 -validity 36500
# keystore.properties en la raíz: storeFile=keystore/release.jks, storePassword, keyAlias=finanzas, keyPassword

./gradlew testDebugUnitTest          # tests de dominio
./gradlew assembleRelease            # → app/build/outputs/apk/release/app-release.apk
```

Instalar en cada teléfono: pasar el APK (cable, Drive, WhatsApp) y abrirlo, aceptando "instalar apps de
origen desconocido". En Xiaomi/HyperOS `adb install` suele quedar bloqueado por la app Seguridad aunque esté
activo "Instalar vía USB": es más simple `adb push app-release.apk /sdcard/Download/` e instalar desde *Archivos*.
Para actualizar, instalar el APK nuevo encima (misma clave de firma); los datos viven en Firestore.

### Actualizaciones por F-Droid (repositorio privado "Nuxapower Apps")

Las versiones se publican en el repositorio F-Droid general de mis apps, que vive en `~/code/nuxapower-apps`
(ver su README). Cada teléfono lo agrega una vez en F-Droid (o Neo Store) con la URL + huella y desde ahí
recibe las actualizaciones.

```
./gradlew :app:assembleRelease      # subir antes versionCode/versionName en app/build.gradle.kts
scripts/publicar-apk.sh             # llama a ~/code/nuxapower-apps/publicar.sh con el APK release
```

Primer uso: una persona toca **Crear hogar** y comparte el código de 6 caracteres (Ajustes → compartir);
la otra lo ingresa en **Unirme**.

## Supuestos

- La fecha del movimiento se guarda como texto `yyyy-MM-dd` (sin zona horaria): agrupar por día y filtrar
  por mes es directo y no cambia si alguien viaja.
- Cada persona es un miembro del hogar y su id es el uid de Firebase. Cada uno edita solo su nombre y color;
  ambos pueden crear, editar y borrar cualquier movimiento y categoría.
- Crear o unirse a un hogar requiere conexión; todo lo demás funciona offline.
- Conocer el código basta para unirse; con 2 miembros el hogar queda cerrado (lo exigen las reglas).
  No hay salida/expulsión de miembros (para dos personas no hace falta; se puede hacer desde la consola).
- Se escucha la colección completa de movimientos y los filtros/resúmenes se calculan en memoria: para dos
  personas son pocos miles al año, bien dentro de la cuota gratuita y más simple que índices compuestos.
- Las categorías no se borran, solo se archivan. El tipo (Gasto/Ingreso) se elige al crearla y luego no cambia.
- Tras guardar, el ingreso rápido limpia monto, categoría, fecha y nota, pero conserva tipo y persona para
  cargar varios seguidos.
- CSV con `;` como separador y BOM UTF-8 (Excel en español lo abre en columnas con acentos correctos);
  montos enteros sin separador de miles.
- El widget lee una copia local de las categorías que la app actualiza al abrirse; si se crean o
  reordenan categorías, el widget se entera la próxima vez que la app corra.
- Fuera de alcance (no implementado): metas, múltiples monedas, fotos, importación bancaria,
  notificaciones, división de deudas. (Presupuestos se agregó después a pedido.)
