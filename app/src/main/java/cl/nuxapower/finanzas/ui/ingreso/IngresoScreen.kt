package cl.nuxapower.finanzas.ui.ingreso

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.ui.comun.DialogoFecha
import cl.nuxapower.finanzas.ui.comun.LocalAvisos
import cl.nuxapower.finanzas.ui.comun.Punto
import cl.nuxapower.finanzas.ui.comun.colorTipo
import cl.nuxapower.finanzas.util.formatoClp
import cl.nuxapower.finanzas.util.formatoDiaCorto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngresoScreen(onCerrar: () -> Unit = {}, vm: IngresoViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val f = e.form
    val avisos = LocalAvisos.current
    val foco = LocalFocusManager.current
    var elegirFecha by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.eventos.collect { ev ->
            when (ev) {
                is EventoIngreso.Guardado -> {
                    foco.clearFocus()
                    avisos.deshacer(ev.mensaje) { vm.deshacerCreacion(ev.id) }
                }
                EventoIngreso.Editado -> {
                    avisos.mostrar("Movimiento actualizado")
                    onCerrar()
                }
                is EventoIngreso.Eliminado -> {
                    avisos.deshacer("Movimiento eliminado") { vm.restaurar(ev.movimiento) }
                    onCerrar()
                }
                EventoIngreso.NoEncontrado -> onCerrar()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (e.editando) {
            TopAppBar(
                title = { Text("Editar movimiento") },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                actions = {
                    IconButton(onClick = vm::eliminar) { Icon(Icons.Filled.Delete, "Eliminar") }
                },
                windowInsets = WindowInsets(0),
            )
        }

        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (!e.editando) Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Tipo.entries.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = f.tipo == t,
                        onClick = { vm.tipo(t) },
                        shape = SegmentedButtonDefaults.itemShape(i, Tipo.entries.size),
                    ) { Text(if (t == Tipo.GASTO) "Gasto" else "Ingreso") }
                }
            }

            Text(
                formatoClp(f.monto),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Medium,
                color = if (f.monto == 0L) MaterialTheme.colorScheme.outline else colorTipo(f.tipo),
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 92.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(e.categorias, key = { it.id }) { c ->
                    CategoriaCelda(c, seleccionada = c.id == f.categoriaId) { vm.categoria(c.id) }
                }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                e.personas.forEach { p ->
                    FilterChip(
                        selected = f.personaId == p.id,
                        onClick = { vm.persona(p.id) },
                        label = { Text(p.nombre) },
                        leadingIcon = { Punto(p.color, 10.dp) },
                    )
                }
                AssistChip(
                    onClick = { elegirFecha = true },
                    label = { Text(formatoDiaCorto(f.fecha)) },
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, null, Modifier.width(18.dp)) },
                )
            }

            OutlinedTextField(
                value = f.nota,
                onValueChange = vm::nota,
                placeholder = { Text("Nota (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            f.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Teclado(onTecla = vm::tecla, onBorrar = vm::borrar, onLimpiar = vm::limpiarMonto)

        Button(
            onClick = vm::guardar,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(56.dp),
        ) { Text(if (e.editando) "Guardar cambios" else "Guardar", style = MaterialTheme.typography.titleMedium) }
    }

    if (elegirFecha) DialogoFecha(f.fecha, onElegir = vm::fecha, onCerrar = { elegirFecha = false })
}

@Composable
private fun CategoriaCelda(c: Categoria, seleccionada: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionada) Color(c.color).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainer,
        border = if (seleccionada) BorderStroke(2.dp, Color(c.color)) else null,
    ) {
        Column(
            Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(c.icono, style = MaterialTheme.typography.headlineSmall)
            Text(
                c.nombre,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Teclado(onTecla: (String) -> Unit, onBorrar: () -> Unit, onLimpiar: () -> Unit) {
    val filas = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("000", "0", "⌫"))
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        filas.forEach { fila ->
            Row(Modifier.fillMaxWidth()) {
                fila.forEach { t ->
                    if (t == "⌫") {
                        Tecla(onClick = onBorrar, onLongClick = onLimpiar) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, "Borrar")
                        }
                    } else {
                        Tecla(onClick = { onTecla(t) }) {
                            Text(t, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.Tecla(onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .height(54.dp)
            .padding(2.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
