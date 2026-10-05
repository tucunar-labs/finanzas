package cl.nuxapower.finanzas.ui.categorias

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.model.paleta
import cl.nuxapower.finanzas.ui.comun.LocalAvisos
import cl.nuxapower.finanzas.ui.comun.Punto
import cl.nuxapower.finanzas.ui.comun.SelectorColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(onVolver: () -> Unit, vm: CategoriasViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val avisos = LocalAvisos.current
    var editando by remember { mutableStateOf<Categoria?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                windowInsets = WindowInsets(0),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Tipo.entries.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = e.tipo == t,
                        onClick = { vm.tipo(t) },
                        shape = SegmentedButtonDefaults.itemShape(i, Tipo.entries.size),
                    ) { Text(if (t == Tipo.GASTO) "Gastos" else "Ingresos") }
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                itemsIndexed(e.activas, key = { _, c -> c.id }) { i, c ->
                    Fila(c, onClick = { editando = c }) {
                        IconButton(onClick = { vm.mover(c, -1) }, enabled = i > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, "Subir")
                        }
                        IconButton(onClick = { vm.mover(c, +1) }, enabled = i < e.activas.lastIndex) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Bajar")
                        }
                        IconButton(onClick = {
                            vm.archivar(c, true)
                            avisos.deshacer("${c.nombre} archivada") { vm.archivar(c, false) }
                        }) { Icon(Icons.Filled.Archive, "Archivar") }
                    }
                }
                if (e.archivadas.isNotEmpty()) {
                    item(key = "titulo-archivadas") {
                        Text(
                            "Archivadas (no aparecen al ingresar; siguen en historial y reportes)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    items(e.archivadas, key = { it.id }) { c ->
                        Fila(c, archivada = true, onClick = { editando = c }) {
                            IconButton(onClick = { vm.archivar(c, false) }) {
                                Icon(Icons.Filled.Unarchive, "Restaurar")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.padding(48.dp)) }
            }
        }
        FloatingActionButton(
            onClick = { editando = Categoria("", "", "", paleta.first(), e.tipo) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, "Nueva categoría") }
    }

    editando?.let { c ->
        DialogoCategoria(
            inicial = c,
            onGuardar = { vm.guardar(it); editando = null },
            onCerrar = { editando = null },
        )
    }
}

@Composable
private fun Fila(
    c: Categoria,
    archivada: Boolean = false,
    onClick: () -> Unit,
    acciones: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(c.icono, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Punto(c.color, 10.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            c.nombre,
            modifier = Modifier.weight(1f),
            color = if (archivada) MaterialTheme.colorScheme.outline else Color.Unspecified,
        )
        acciones()
    }
}

@Composable
private fun DialogoCategoria(inicial: Categoria, onGuardar: (Categoria) -> Unit, onCerrar: () -> Unit) {
    var nombre by remember { mutableStateOf(inicial.nombre) }
    var icono by remember { mutableStateOf(inicial.icono) }
    var color by remember { mutableStateOf(inicial.color) }
    var tipo by remember { mutableStateOf(inicial.tipo) }
    val nueva = inicial.id.isEmpty()
    val valido = nombre.isNotBlank() && icono.isNotBlank()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (nueva) "Nueva categoría" else "Editar categoría") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = icono,
                        onValueChange = { icono = it.take(8) },
                        label = { Text("Emoji") },
                        singleLine = true,
                        modifier = Modifier.width(88.dp),
                    )
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it.take(40) },
                        label = { Text("Nombre") },
                        singleLine = true,
                    )
                }
                // El tipo solo se elige al crear: cambiarlo después movería la categoría de lista.
                if (nueva) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Tipo.entries.forEachIndexed { i, t ->
                            SegmentedButton(
                                selected = tipo == t,
                                onClick = { tipo = t },
                                shape = SegmentedButtonDefaults.itemShape(i, Tipo.entries.size),
                            ) { Text(if (t == Tipo.GASTO) "Gasto" else "Ingreso") }
                        }
                    }
                }
                SelectorColor(color) { color = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = { onGuardar(inicial.copy(nombre = nombre, icono = icono.trim(), color = color, tipo = tipo)) },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}
