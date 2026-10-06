package cl.nuxapower.finanzas.ui.ingreso

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
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
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.material3.SuggestionChip
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
import cl.nuxapower.finanzas.ui.theme.ColoresMonto
import cl.nuxapower.finanzas.util.formatoClp
import cl.nuxapower.finanzas.util.formatoMes
import cl.nuxapower.finanzas.util.formatoDiaCorto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngresoScreen(onCerrar: () -> Unit = {}, vm: IngresoViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val f = e.form
    val avisos = LocalAvisos.current
    val foco = LocalFocusManager.current
    var elegirFecha by remember { mutableStateOf(false) }
    var editarNota by remember { mutableStateOf(false) }

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
                is EventoIngreso.Duplicado -> {
                    avisos.deshacer("Duplicado con fecha de hoy") { vm.deshacerCreacion(ev.id) }
                    onCerrar()
                }
            }
        }
    }

    // En pantallas bajas (o con letra grande) se achican teclado y monto para que las categorías se vean.
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compacto = maxHeight < 720.dp
    val altoTecla = if (compacto) 44.dp else 52.dp
    Column(Modifier.fillMaxSize()) {
        if (e.editando) {
            TopAppBar(
                title = { Text("Editar movimiento") },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                actions = {
                    IconButton(onClick = vm::duplicar) { Icon(Icons.Filled.ContentCopy, "Duplicar con fecha de hoy") }
                    IconButton(onClick = vm::eliminar) { Icon(Icons.Filled.Delete, "Eliminar") }
                },
                windowInsets = WindowInsets(0),
            )
        }

        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (!e.editando) Spacer(Modifier.height(if (compacto) 4.dp else 12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Tipo.entries.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = f.tipo == t,
                        onClick = { vm.tipo(t) },
                        shape = SegmentedButtonDefaults.itemShape(i, Tipo.entries.size),
                    ) { Text(if (t == Tipo.GASTO) "Gasto" else "Ingreso") }
                }
            }

            if (f.sumandos.isNotEmpty()) {
                Text(
                    (f.sumandos.map { formatoClp(it) } + listOfNotNull(f.digitos.toLongOrNull()?.let { formatoClp(it) }))
                        .joinToString(" + "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            Text(
                formatoClp(f.monto),
                style = if (compacto) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Medium,
                color = if (f.monto == 0L) MaterialTheme.colorScheme.outline else colorTipo(f.tipo),
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(vertical = if (compacto) 2.dp else 8.dp),
            )

            e.contexto?.let { LineaContexto(it) }

            // El espacio libre queda arriba: categorías, chips y teclado juntos abajo, al alcance del pulgar.
            Spacer(Modifier.weight(1f))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // Hasta ~3 filas visibles; con más categorías la grilla hace scroll.
                modifier = Modifier.heightIn(max = if (compacto) 150.dp else 230.dp),
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
                // La nota va en un diálogo: así el teclado del sistema no tapa categorías ni monto.
                FilterChip(
                    selected = f.nota.isNotBlank(),
                    onClick = { editarNota = true },
                    label = {
                        Text(
                            f.nota.ifBlank { "Nota" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp),
                        )
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null, Modifier.width(18.dp)) },
                )
            }

            f.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Teclado(
            onTecla = vm::tecla,
            onSumar = vm::sumar,
            onBorrar = vm::borrar,
            onLimpiar = vm::limpiarMonto,
            altoTecla = altoTecla,
        )

        Button(
            onClick = vm::guardar,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compacto) 4.dp else 8.dp)
                .height(if (compacto) 48.dp else 56.dp),
        ) { Text(if (e.editando) "Guardar cambios" else "Guardar", style = MaterialTheme.typography.titleMedium) }
    }
    }

    if (elegirFecha) DialogoFecha(f.fecha, onElegir = vm::fecha, onCerrar = { elegirFecha = false })
    if (editarNota) {
        DialogoNota(
            inicial = f.nota,
            sugerencias = e.sugerencias,
            onGuardar = { vm.nota(it); editarNota = false },
            onCerrar = { editarNota = false },
        )
    }
}

/** "Octubre: $185.100 en gastos" o "🛒 Supermercado: quedan $228.900 → quedarían $216.900". */
@Composable
private fun LineaContexto(c: Contexto) {
    val (texto, color) = when (c) {
        is Contexto.TotalMes -> {
            val que = if (c.tipo == Tipo.GASTO) "en gastos" else "en ingresos"
            "${formatoMes(c.mes)}: ${formatoClp(c.total)} $que" to MaterialTheme.colorScheme.outline
        }
        is Contexto.Presupuesto -> {
            val base = "${c.categoria.icono} ${c.categoria.nombre}: " +
                if (c.restante >= 0) "quedan ${formatoClp(c.restante)}" else "pasados por ${formatoClp(-c.restante)}"
            val despues = c.despues?.let {
                if (it >= 0) " → quedarían ${formatoClp(it)}" else " → te pasarías por ${formatoClp(-it)}"
            }.orEmpty()
            val pasado = (c.despues ?: c.restante) < 0
            base + despues to if (pasado) ColoresMonto.gasto() else MaterialTheme.colorScheme.outline
        }
    }
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.End,
        maxLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DialogoNota(inicial: String, sugerencias: List<String>, onGuardar: (String) -> Unit, onCerrar: () -> Unit) {
    var texto by remember { mutableStateOf(inicial) }
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.take(500) },
                    placeholder = { Text("Opcional") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onGuardar(texto.trim()) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(foco),
                )
                if (sugerencias.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sugerencias.forEach { s ->
                            SuggestionChip(onClick = { onGuardar(s) }, label = { Text(s, maxLines = 1) })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(texto.trim()) }) { Text("Listo") } },
        dismissButton = {
            if (inicial.isNotEmpty()) TextButton(onClick = { onGuardar("") }) { Text("Quitar") }
            else TextButton(onClick = onCerrar) { Text("Cancelar") }
        },
    )
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
            Modifier.padding(vertical = 6.dp, horizontal = 2.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(c.icono, style = MaterialTheme.typography.titleLarge)
            Text(
                c.nombre,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Teclado numérico: "+" suma varios montos, ⌫ borra (mantener presionado = limpiar). */
@Composable
internal fun Teclado(
    onTecla: (String) -> Unit,
    onSumar: () -> Unit,
    onBorrar: () -> Unit,
    onLimpiar: () -> Unit,
    altoTecla: Dp = 52.dp,
) {
    @Composable
    fun RowScope.Digito(t: String) = Tecla(onClick = { onTecla(t) }, alto = altoTecla) {
        Text(t, style = MaterialTheme.typography.headlineSmall)
    }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { fila ->
            Row(Modifier.fillMaxWidth()) { fila.forEach { Digito(it) } }
        }
        Row(Modifier.fillMaxWidth()) {
            Tecla(onClick = onSumar, alto = altoTecla) { Text("+", style = MaterialTheme.typography.headlineSmall) }
            Digito("0")
            Tecla(onClick = onBorrar, onLongClick = onLimpiar, alto = altoTecla) { Icon(Icons.AutoMirrored.Filled.Backspace, "Borrar") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.Tecla(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    alto: Dp = 52.dp,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .weight(1f)
            .height(alto)
            .padding(2.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
