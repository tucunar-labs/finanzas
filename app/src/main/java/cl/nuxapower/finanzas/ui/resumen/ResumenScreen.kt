package cl.nuxapower.finanzas.ui.resumen

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.domain.Parte
import cl.nuxapower.finanzas.domain.PuntoMes
import cl.nuxapower.finanzas.domain.Resumen
import cl.nuxapower.finanzas.domain.variacion
import cl.nuxapower.finanzas.ui.comun.Punto
import cl.nuxapower.finanzas.ui.comun.SelectorMes
import cl.nuxapower.finanzas.ui.theme.ColoresMonto
import cl.nuxapower.finanzas.util.formatoClp
import cl.nuxapower.finanzas.util.localeCl
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ResumenScreen(onVerMovimientos: () -> Unit, vm: ResumenViewModel = hiltViewModel()) {
    val r by vm.resumen.collectAsStateWithLifecycle()
    val mes by vm.mesActual.collectAsStateWithLifecycle()
    var tipoCategorias by rememberSaveable { mutableStateOf(Tipo.GASTO) }
    fun ver(tipo: Tipo, categoriaId: String? = null, personaId: String? = null) {
        vm.verMovimientos(tipo, categoriaId, personaId)
        onVerMovimientos()
    }

    Column(Modifier.fillMaxSize()) {
        SelectorMes(mes, vm::mes, Modifier.padding(horizontal = 4.dp))
        val res = r
        if (res == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            // "vs 1–6 sep" en el mes en curso (mismo período), "vs septiembre" en meses cerrados.
            val anterior = res.mes.minusMonths(1)
            val etiqueta = res.corteDia?.let { "1–$it ${anterior.format(DateTimeFormatter.ofPattern("MMM", localeCl))}" }
                ?: anterior.format(DateTimeFormatter.ofPattern("MMMM", localeCl))
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Totales(res, etiqueta)
                Seccion("Por categoría") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Tipo.entries.forEachIndexed { i, t ->
                            SegmentedButton(
                                selected = tipoCategorias == t,
                                onClick = { tipoCategorias = t },
                                shape = SegmentedButtonDefaults.itemShape(i, Tipo.entries.size),
                            ) { Text(if (t == Tipo.GASTO) "Gastos" else "Ingresos") }
                        }
                    }
                    val partes = if (tipoCategorias == Tipo.GASTO) res.gastoPorCategoria else res.ingresoPorCategoria
                    if (partes.isEmpty()) SinDatos(tipoCategorias)
                    partes.forEach { p ->
                        Barra(p, anterior = etiqueta) { ver(tipoCategorias, categoriaId = p.id) }
                    }
                }
                Seccion("Gasto por persona") {
                    if (res.gastoPorPersona.isEmpty()) SinDatos(Tipo.GASTO)
                    res.gastoPorPersona.forEach { p ->
                        Barra(p, anterior = null) { ver(Tipo.GASTO, personaId = p.id) }
                    }
                }
                Seccion("Últimos 6 meses") { Tendencia(res.tendencia) }
            }
        }
    }
}

@Composable
private fun Totales(r: Resumen, etiqueta: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FilaTotal("Ingresos", r.ingresos, r.ingresosMesAnterior, ColoresMonto.ingreso(), subirEsBueno = true, etiqueta = etiqueta)
            FilaTotal("Gastos", r.gastos, r.gastosMesAnterior, ColoresMonto.gasto(), subirEsBueno = false, etiqueta = etiqueta)
            FilaTotal(
                "Balance",
                r.balance,
                r.balanceMesAnterior,
                if (r.balance >= 0) ColoresMonto.ingreso() else ColoresMonto.gasto(),
                subirEsBueno = true,
                etiqueta = etiqueta,
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
    etiqueta: String,
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
            comparacion(monto, anterior, etiqueta),
            style = MaterialTheme.typography.bodySmall,
            color = colorComparacion(monto - anterior, subirEsBueno),
            modifier = Modifier.align(Alignment.End),
        )
    }
}

/** "▲ 12% vs 1–6 sep ($450.000)" */
private fun comparacion(actual: Long, anterior: Long, etiqueta: String): String {
    val dif = actual - anterior
    val flecha = when {
        dif > 0 -> "▲"
        dif < 0 -> "▼"
        else -> "="
    }
    val pct = variacion(actual, anterior)?.let { " ${kotlin.math.abs(it)}%" } ?: ""
    return "$flecha$pct vs $etiqueta (${formatoClp(anterior)})"
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
private fun SinDatos(tipo: Tipo) =
    Text(if (tipo == Tipo.GASTO) "Sin gastos este mes" else "Sin ingresos este mes", color = MaterialTheme.colorScheme.outline)

/** Barras verticales ingresos (verde) vs gastos (rojo) por mes, escaladas al máximo del período. */
@Composable
private fun Tendencia(puntos: List<PuntoMes>) {
    val max = puntos.maxOfOrNull { maxOf(it.ingresos, it.gastos) }?.takeIf { it > 0 } ?: 1L
    val alto = 120.dp
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        puntos.forEach { p ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.height(alto), verticalAlignment = Alignment.Bottom) {
                    BarraVertical(p.ingresos.toFloat() / max, alto, ColoresMonto.ingreso())
                    Spacer(Modifier.width(3.dp))
                    BarraVertical(p.gastos.toFloat() / max, alto, ColoresMonto.gasto())
                }
                Text(
                    p.mes.format(DateTimeFormatter.ofPattern("MMM", localeCl)).take(3),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    formatoCorto(p.ingresos - p.gastos),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (p.ingresos >= p.gastos) ColoresMonto.ingreso() else ColoresMonto.gasto(),
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
        Punto(0xFF1E8E3E, 8.dp); Text(" Ingresos   ", style = MaterialTheme.typography.labelSmall)
        Punto(0xFFC5221F, 8.dp); Text(" Gastos   · balance bajo cada mes", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BarraVertical(fraccion: Float, alto: androidx.compose.ui.unit.Dp, color: Color) {
    Box(
        Modifier.width(12.dp).height(alto * fraccion.coerceIn(0f, 1f))
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(color)
    )
}

/** Monto compacto para ejes: "$1,2M", "$350k", "-$40k". */
private fun formatoCorto(m: Long): String {
    val signo = if (m < 0) "-" else ""
    val a = kotlin.math.abs(m)
    return signo + when {
        a >= 1_000_000 -> "$" + String.format(localeCl, "%.1fM", a / 1_000_000.0).replace(".0M", "M")
        a >= 1_000 -> "$" + (a / 1_000) + "k"
        else -> "$" + a
    }
}

/** Barra horizontal con porcentaje del total de gastos del mes. */
@Composable
private fun Barra(p: Parte, anterior: String?, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
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
        if (anterior != null) {
            val pct = variacion(p.monto, p.montoMesAnterior)
            Text(
                "${anterior.replaceFirstChar { it.titlecase(localeCl) }}: ${formatoClp(p.montoMesAnterior)}" +
                    (pct?.let { " (${if (it > 0) "+" else ""}$it%)" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
