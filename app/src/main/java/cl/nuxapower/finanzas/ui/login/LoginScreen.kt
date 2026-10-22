package cl.nuxapower.finanzas.ui.login

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.BuildConfig
import cl.nuxapower.finanzas.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(vm: LoginViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(140.dp))
        Spacer(Modifier.height(16.dp))
        Text("Finanzas del hogar", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Gastos e ingresos compartidos, para dos.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        if (estado.cargando) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                scope.launch {
                    vm.iniciando()
                    runCatching { obtenerIdToken(context) }
                        .onSuccess { vm.entrar(it) }
                        .onFailure { vm.falloCredencial(it) }
                }
            }) { Text("Entrar con Google") }
            // Solo en debug con emuladores (en release la condición es constante y R8 lo elimina).
            if (BuildConfig.EMULADOR_HOST.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = vm::entrarDev) { Text("Entrar como dev@example.com (dev)") }
            }
        }
        estado.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}

/** Abre el selector de cuentas de Google (Credential Manager) y devuelve el ID token. */
private suspend fun obtenerIdToken(context: Context): String {
    val opcion = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(opcion).build()
    val cred = CredentialManager.create(context).getCredential(context, request).credential
    check(cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        "Credencial inesperada"
    }
    return GoogleIdTokenCredential.createFrom(cred.data).idToken
}

internal fun mensajeCredencial(e: Throwable): String? = when (e) {
    is GetCredentialCancellationException -> null
    else -> "No se pudo iniciar sesión: ${e.message}"
}
