package cl.nuxapower.finanzas.ui.resumen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.domain.Parte
import cl.nuxapower.finanzas.domain.Resumen
import cl.nuxapower.finanzas.domain.variacion
import cl.nuxapower.finanzas.ui.comun.Punto
import cl.nuxapower.finanzas.ui.comun.SelectorMes
import cl.nuxapower.finanzas.ui.theme.ColoresMonto
import cl.nuxapower.finanzas.util.formatoClp
import kotlin.math.roundToInt

@Composable
fun ResumenScreen(vm: ResumenViewModel = hiltViewModel()) {
    val r by vm.resumen.collectAsStateWithLifecycle()
    val mes by vm.mesActual.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SelectorMes(mes, vm::mes, Modifier.padding(horizontal = 4.dp))
        val res = r
        if (res == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Totales(res)
                Seccion("Gasto por categoría") {
                    if (res.gastoPorCategoria.isEmpty()) SinDatos()
                    res.gastoPorCategoria.forEach { Barra(it, mostrarAnterior = true) }
                }
                Seccion("Gasto por persona") {
                    if (res.gastoPorPersona.isEmpty()) SinDatos()
                    res.gastoPorPersona.forEach { Barra(it, mostrarAnterior = false) }
                }
            }
        }
    }
}

@Composable
private fun Totales(r: Resumen) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FilaTotal("Ingresos", r.ingresos, r.ingresosMesAnterior, ColoresMonto.ingreso(), subirEsBueno = true)
            FilaTotal("Gastos", r.gastos, r.gastosMesAnterior, ColoresMonto.gasto(), subirEsBueno = false)
            FilaTotal(
                "Balance",
                r.balance,
                r.balanceMesAnterior,
                if (r.balance >= 0) ColoresMonto.ingreso() else ColoresMonto.gasto(),
                subirEsBueno = true,
                destacado = true,
            )
        }
    }
}

@Composable
private fun FilaTotal(
    titulo: String,
    monto: Long,
    anterior: Long,
    color: Color,
    subirEsBueno: Boolean,
    destacado: Boolean = false,
) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                formatoClp(monto),
                style = if (destacado) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                fontWeight = if (destacado) FontWeight.Bold else FontWeight.Medium,
                color = color,
            )
        }
        Text(
            comparacion(monto, anterior),
            style = MaterialTheme.typography.bodySmall,
            color = colorComparacion(monto - anterior, subirEsBueno),
            modifier = Modifier.align(Alignment.End),
        )
    }
}

/** "▲ 12% vs mes anterior ($450.000)" */
private fun comparacion(actual: Long, anterior: Long): String {
    val dif = actual - anterior
    val flecha = when {
        dif > 0 -> "▲"
        dif < 0 -> "▼"
        else -> "="
    }
    val pct = variacion(actual, anterior)?.let { " ${kotlin.math.abs(it)}%" } ?: ""
    return "$flecha$pct vs mes anterior (${formatoClp(anterior)})"
}

@Composable
private fun colorComparacion(dif: Long, subirEsBueno: Boolean): Color = when {
    dif == 0L -> MaterialTheme.colorScheme.outline
    (dif > 0) == subirEsBueno -> ColoresMonto.ingreso()
    else -> ColoresMonto.gasto()
}

@Composable
private fun Seccion(titulo: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SinDatos() = Text("Sin gastos este mes", color = MaterialTheme.colorScheme.outline)

/** Barra horizontal con porcentaje del total de gastos del mes. */
@Composable
private fun Barra(p: Parte, mostrarAnterior: Boolean) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (p.icono.isNotEmpty()) Text(p.icono) else Punto(p.color)
            Spacer(Modifier.width(8.dp))
            Text(p.nombre, modifier = Modifier.weight(1f))
            Text(
                "${(p.fraccion * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.width(8.dp))
            Text(formatoClp(p.monto), fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier.fillMaxWidth(p.fraccion.coerceIn(0f, 1f)).height(8.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Color(p.color))
            )
        }
        if (mostrarAnterior) {
            val pct = variacion(p.monto, p.montoMesAnterior)
            Text(
                "Mes anterior: ${formatoClp(p.montoMesAnterior)}" +
                    (pct?.let { " (${if (it > 0) "+" else ""}$it%)" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
