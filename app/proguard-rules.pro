# Los modelos se mapean a mano desde/hacia Map, así que no hace falta conservarlos.
# Credential Manager / Google ID
-keep class com.google.android.libraries.identity.googleid.** { *; }
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }

# WorkManager (lo usa Glance para el widget): Room instancia WorkDatabase_Impl por reflexión.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
# WorkManager crea InputMergers y Workers por reflexión; sus reglas propias no alcanzan con AGP 9.
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
