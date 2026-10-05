package cl.nuxapower.finanzas.ui.presupuesto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.domain.EstadoPresupuesto
import cl.nuxapower.finanzas.domain.LineaPresupuesto
import cl.nuxapower.finanzas.ui.comun.SelectorMes
import cl.nuxapower.finanzas.ui.theme.ColoresMonto
import cl.nuxapower.finanzas.util.formatoClp
import cl.nuxapower.finanzas.util.formatoMes
import kotlin.math.roundToInt

@Composable
fun PresupuestoScreen(onVerMovimientos: () -> Unit, vm: PresupuestoViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val mes by vm.mesActual.collectAsStateWithLifecycle()
    var editando by remember { mutableStateOf<LineaPresupuesto?>(null) }

    Column(Modifier.fillMaxSize()) {
        SelectorMes(mes, vm::mes, Modifier.padding(horizontal = 4.dp))
        val est = e
        if (est == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Total(est)
            if (est.origen != null && est.origen != est.mes) {
                Text(
                    "Usando los montos de ${formatoMes(est.origen)}. Si cambias uno, se guarda solo para este mes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (est.conPresupuesto.isEmpty()) {
                Text(
                    "Toca una categoría para definir cuánto quieren gastar al mes.",
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            est.conPresupuesto.forEach { l ->
                FilaPresupuesto(l, onEditar = { editando = l }, onVer = {
                    vm.verMovimientos(l.categoria.id)
                    onVerMovimientos()
                })
            }
            if (est.sinPresupuesto.isNotEmpty()) {
                Text(
                    "Sin presupuesto",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                est.sinPresupuesto.forEach { l ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { editando = l }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(l.categoria.icono)
                        Spacer(Modifier.width(12.dp))
                        Text(l.categoria.nombre, modifier = Modifier.weight(1f))
                        if (l.gastado > 0) {
                            Text(formatoClp(l.gastado), color = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Definir", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    editando?.let { l ->
        DialogoMonto(
            linea = l,
            onGuardar = { vm.definir(l.categoria.id, it); editando = null },
            onCerrar = { editando = null },
        )
    }
}

/** Verde hasta 80 %, ámbar hasta 100 %, rojo al pasarse. */
@Composable
private fun colorAvance(fraccion: Float): Color = when {
    fraccion > 1f -> ColoresMonto.gasto()
    fraccion >= 0.8f -> Color(0xFFF9A825)
    else -> ColoresMonto.ingreso()
}

@Composable
private fun BarraAvance(fraccion: Float, alto: androidx.compose.ui.unit.Dp = 10.dp) {
    Box(
        Modifier.fillMaxWidth().height(alto).clip(RoundedCornerShape(alto / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier.fillMaxWidth(fraccion.coerceIn(0f, 1f)).height(alto)
                .clip(RoundedCornerShape(alto / 2)).background(colorAvance(fraccion))
        )
    }
}

@Composable
private fun Total(e: EstadoPresupuesto) {
    if (e.totalPresupuesto == 0L) return
    val fraccion = e.totalGastadoPresupuestado.toFloat() / e.totalPresupuesto
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Total del mes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "${formatoClp(e.totalGastadoPresupuestado)} de ${formatoClp(e.totalPresupuesto)}",
                    fontWeight = FontWeight.Medium,
                )
            }
            BarraAvance(fraccion, 14.dp)
            val restante = e.totalPresupuesto - e.totalGastadoPresupuestado
            Text(
                if (restante >= 0) "Quedan ${formatoClp(restante)}" else "Se pasaron por ${formatoClp(-restante)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (restante >= 0) MaterialTheme.colorScheme.outline else ColoresMonto.gasto(),
            )
            if (e.excedidas > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = ColoresMonto.gasto(), modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (e.excedidas == 1) "1 categoría excedida" else "${e.excedidas} categorías excedidas",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColoresMonto.gasto(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaPresupuesto(l: LineaPresupuesto, onEditar: () -> Unit, onVer: () -> Unit) {
    val excedido = l.excedido
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onVer),
        colors = if (excedido) CardDefaults.cardColors(containerColor = ColoresMonto.gasto().copy(alpha = 0.12f))
        else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l.categoria.icono, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(l.categoria.nombre, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${formatoClp(l.gastado)} de ${formatoClp(l.presupuesto)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    "${(l.fraccion * 100).roundToInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = colorAvance(l.fraccion),
                )
                IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, "Editar presupuesto") }
            }
            Box(Modifier.padding(end = 12.dp)) { BarraAvance(l.fraccion) }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (excedido) {
                    Icon(Icons.Filled.Warning, null, tint = ColoresMonto.gasto(), modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    if (excedido) "Se pasaron por ${formatoClp(-l.restante)}" else "Quedan ${formatoClp(l.restante)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (excedido) ColoresMonto.gasto() else MaterialTheme.colorScheme.outline,
                    fontWeight = if (excedido) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun DialogoMonto(linea: LineaPresupuesto, onGuardar: (Long) -> Unit, onCerrar: () -> Unit) {
    var texto by remember { mutableStateOf(if (linea.presupuesto > 0) linea.presupuesto.toString() else "") }
    val monto = texto.toLongOrNull() ?: 0L
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("${linea.categoria.icono} ${linea.categoria.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { v -> texto = v.filter { it.isDigit() }.trimStart('0').take(12) },
                    label = { Text("Presupuesto mensual") },
                    prefix = { Text("$") },
                    supportingText = { Text(if (monto > 0) formatoClp(monto) else "Vacío = sin presupuesto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                if (linea.gastado > 0) {
                    Text(
                        "Gastado este mes: ${formatoClp(linea.gastado)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(monto) }) { Text("Guardar") } },
        dismissButton = {
            Row {
                if (linea.tienePresupuesto) TextButton(onClick = { onGuardar(0) }) { Text("Quitar") }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        },
    )
}
