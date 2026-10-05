package cl.nuxapower.finanzas.ui.ajustes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.ui.comun.LocalAvisos
import cl.nuxapower.finanzas.ui.comun.Punto
import cl.nuxapower.finanzas.ui.comun.SelectorColor
import cl.nuxapower.finanzas.util.aFechaUtc
import kotlinx.coroutines.launch

@Composable
fun AjustesScreen(onCategorias: () -> Unit, vm: AjustesViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val scope = rememberCoroutineScope()
    var editarPerfil by remember { mutableStateOf(false) }
    var elegirRango by remember { mutableStateOf(false) }

    fun compartir(uri: Uri?) {
        if (uri == null) {
            avisos.mostrar("Aún no cargan los movimientos")
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exportar movimientos"))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Hogar", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                e.personas.forEach { p ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Punto(p.color, 14.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(p.nombre + if (p.id == e.uid) " (tú)" else "", modifier = Modifier.weight(1f))
                        if (p.id == e.uid) {
                            IconButton(onClick = { editarPerfil = true }) { Icon(Icons.Filled.Edit, "Editar perfil") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Código de invitación", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        e.hogar?.codigoInvitacion ?: "…",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        val txt = "Únete a nuestro hogar en Finanzas con el código ${e.hogar?.codigoInvitacion}"
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, txt),
                                "Compartir código",
                            )
                        )
                    }) { Icon(Icons.Filled.Share, "Compartir código") }
                }
                Text(
                    "Miembros: ${e.hogar?.miembros?.size ?: 0} de 2",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Categorías") },
                supportingContent = { Text("Crear, editar, archivar y ordenar") },
                leadingContent = { Icon(Icons.Filled.Category, null) },
                trailingContent = {
                    IconButton(onClick = onCategorias) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Abrir") }
                },
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Download, null)
                    Spacer(Modifier.width(12.dp))
                    Text("Exportar a CSV", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "${e.totalMovimientos} movimientos en total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Row {
                    TextButton(onClick = { scope.launch { compartir(vm.exportar(null, null)) } }) { Text("Todo") }
                    TextButton(onClick = { elegirRango = true }) { Text("Rango de fechas…") }
                }
            }
        }

        TextButton(onClick = vm::salir) {
            Icon(Icons.AutoMirrored.Filled.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
    }

    if (editarPerfil) {
        e.yo?.let { yo ->
            DialogoPerfil(yo.nombre, yo.color, onGuardar = { n, c ->
                vm.guardarPerfil(n, c)
                editarPerfil = false
            }, onCerrar = { editarPerfil = false })
        }
    }

    if (elegirRango) {
        DialogoRango(
            onElegir = { desde, hasta -> scope.launch { compartir(vm.exportar(desde, hasta)) } },
            onCerrar = { elegirRango = false },
        )
    }
}

@Composable
private fun DialogoPerfil(nombreInicial: String, colorInicial: Long, onGuardar: (String, Long) -> Unit, onCerrar: () -> Unit) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var color by remember { mutableStateOf(colorInicial) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Tu perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.take(40) },
                    label = { Text("Nombre") },
                    singleLine = true,
                )
                SelectorColor(color) { color = it }
            }
        },
        confirmButton = {
            TextButton(enabled = nombre.isNotBlank(), onClick = { onGuardar(nombre, color) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoRango(onElegir: (java.time.LocalDate, java.time.LocalDate) -> Unit, onCerrar: () -> Unit) {
    val state = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null,
                onClick = {
                    val desde = state.selectedStartDateMillis!!.aFechaUtc()
                    val hasta = state.selectedEndDateMillis?.aFechaUtc() ?: desde
                    onElegir(desde, hasta)
                    onCerrar()
                },
            ) { Text("Exportar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f))
    }
}
