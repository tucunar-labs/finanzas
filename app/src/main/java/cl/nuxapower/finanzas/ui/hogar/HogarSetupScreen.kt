package cl.nuxapower.finanzas.ui.hogar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HogarSetupScreen(vm: HogarSetupViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Tu hogar", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Crea uno nuevo o únete al de tu pareja con su código.",
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        if (estado.cargando) {
            CircularProgressIndicator()
        } else {
            Button(onClick = vm::crear, modifier = Modifier.fillMaxWidth()) { Text("Crear hogar") }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = estado.codigo,
                onValueChange = vm::onCodigo,
                label = { Text("Código de invitación") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { vm.unirse() }),
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = vm::unirse,
                enabled = estado.codigo.length == 6,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Unirme") }
        }

        estado.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = vm::salir) { Text("Cerrar sesión") }
    }
}
