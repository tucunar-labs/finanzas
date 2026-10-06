package cl.nuxapower.finanzas.ui.movimientos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.ui.comun.LocalAvisos
import cl.nuxapower.finanzas.ui.comun.MontoTipo
import cl.nuxapower.finanzas.ui.comun.Punto
import cl.nuxapower.finanzas.ui.comun.SelectorMes
import cl.nuxapower.finanzas.ui.theme.ColoresMonto
import cl.nuxapower.finanzas.util.formatoClp
import cl.nuxapower.finanzas.util.formatoDia

@Composable
fun MovimientosScreen(onEditar: (String) -> Unit, vm: MovimientosViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val avisos = LocalAvisos.current

    var buscando by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        if (buscando) {
            OutlinedTextField(
                value = e.filtros.busqueda,
                onValueChange = vm::buscar,
                placeholder = { Text("Buscar en todos los meses") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    IconButton(onClick = { vm.buscar(""); buscando = false }) { Icon(Icons.Filled.Close, "Cerrar búsqueda") }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SelectorMes(e.filtros.mes, vm::mes, Modifier.weight(1f).padding(start = 4.dp))
                IconButton(onClick = { buscando = true }) { Icon(Icons.Filled.Search, "Buscar") }
            }
        }
        BarraFiltros(e, vm)
        if (e.pendientes > 0) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CloudOff, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${e.pendientes} sin sincronizar: se subirán solos al volver la conexión",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Ingresos ", style = MaterialTheme.typography.bodyMedium)
            MontoTipo(e.totalIngresos, Tipo.INGRESO, MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text("Gastos ", style = MaterialTheme.typography.bodyMedium)
            MontoTipo(e.totalGastos, Tipo.GASTO, MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider()

        when {
            e.cargando -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            e.dias.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sin movimientos", color = MaterialTheme.colorScheme.outline)
                    val f = e.filtros
                    if (f.tipo != null || f.categoriaId != null || f.personaId != null) {
                        TextButton(onClick = vm::limpiarFiltros) { Text("Quitar filtros") }
                    }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                e.dias.forEach { dia ->
                    item(key = "dia-${dia.fecha}") {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Text(
                                formatoDia(dia.fecha) + if (e.filtros.busqueda.isNotBlank()) " ${dia.fecha.year}" else "",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                (if (dia.neto > 0) "+" else "") + formatoClp(dia.neto),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    items(dia.movimientos, key = { it.id }) { m ->
                        FilaDeslizable(
                            m = m,
                            categoria = e.categorias[m.categoriaId],
                            persona = e.personas[m.personaId],
                            onClick = { onEditar(m.id) },
                            onEliminar = {
                                vm.eliminar(m)
                                avisos.deshacer("Movimiento eliminado") { vm.restaurar(m) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarraFiltros(e: MovimientosEstado, vm: MovimientosViewModel) {
    var menuTipo by remember { mutableStateOf(false) }
    var menuCategoria by remember { mutableStateOf(false) }
    val f = e.filtros

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            FilterChip(
                selected = f.tipo != null,
                onClick = { menuTipo = true },
                label = { Text(when (f.tipo) { null -> "Tipo"; Tipo.GASTO -> "Gastos"; Tipo.INGRESO -> "Ingresos" }) },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
            )
            DropdownMenu(menuTipo, onDismissRequest = { menuTipo = false }) {
                listOf(null to "Todos", Tipo.GASTO to "Gastos", Tipo.INGRESO to "Ingresos").forEach { (t, txt) ->
                    DropdownMenuItem(text = { Text(txt) }, onClick = { vm.tipo(t); menuTipo = false })
                }
            }
        }
        Box {
            val cat = e.categorias[f.categoriaId]
            FilterChip(
                selected = cat != null,
                onClick = { menuCategoria = true },
                label = { Text(cat?.let { "${it.icono} ${it.nombre}" } ?: "Categoría") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
            )
            DropdownMenu(menuCategoria, onDismissRequest = { menuCategoria = false }) {
                DropdownMenuItem(text = { Text("Todas") }, onClick = { vm.categoria(null); menuCategoria = false })
                e.categoriasFiltro.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.icono} ${c.nombre}" + if (c.archivada) " (archivada)" else "") },
                        onClick = { vm.categoria(c.id); menuCategoria = false },
                    )
                }
            }
        }
        e.personas.values.sortedBy { it.nombre }.forEach { p ->
            FilterChip(
                selected = f.personaId == p.id,
                onClick = { vm.persona(if (f.personaId == p.id) null else p.id) },
                label = { Text(p.nombre) },
                leadingIcon = { Punto(p.color, 10.dp) },
            )
        }
    }
}

@Composable
private fun FilaDeslizable(
    m: Movimiento,
    categoria: Categoria?,
    persona: Persona?,
    onClick: () -> Unit,
    onEliminar: () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onEliminar()
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(ColoresMonto.gasto()).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(Icons.Filled.Delete, "Eliminar", tint = MaterialTheme.colorScheme.surface) }
        },
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(categoria?.icono ?: "❔", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(categoria?.nombre ?: "Sin categoría", style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    persona?.let {
                        Punto(it.color, 8.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(it.nombre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    if (m.nota.isNotBlank()) {
                        Text(
                            " · ${m.nota}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (m.pendiente) {
                Icon(
                    Icons.Filled.CloudOff,
                    "Sin sincronizar",
                    Modifier.size(14.dp).padding(end = 2.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.width(4.dp))
            }
            MontoTipo(m.monto, m.tipo)
        }
    }
}
