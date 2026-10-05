# Los modelos se mapean a mano desde/hacia Map, así que no hace falta conservarlos.
# Credential Manager / Google ID
-keep class com.google.android.libraries.identity.googleid.** { *; }
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
