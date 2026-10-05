package cl.nuxapower.finanzas.ui.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.model.paleta
import cl.nuxapower.finanzas.ui.theme.ColoresMonto
import cl.nuxapower.finanzas.util.aFechaUtc
import cl.nuxapower.finanzas.util.aMillisUtc
import cl.nuxapower.finanzas.util.formatoClp
import cl.nuxapower.finanzas.util.formatoMes
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun Punto(color: Long, size: Dp = 12.dp) {
    Box(Modifier.size(size).background(Color(color), CircleShape))
}

@Composable
fun colorTipo(tipo: Tipo): Color = if (tipo == Tipo.GASTO) ColoresMonto.gasto() else ColoresMonto.ingreso()

/** Monto con signo y color según tipo: "-$12.500" en rojo, "+$500.000" en verde. */
@Composable
fun MontoTipo(monto: Long, tipo: Tipo, style: TextStyle = MaterialTheme.typography.bodyLarge) {
    Text(
        (if (tipo == Tipo.GASTO) "-" else "+") + formatoClp(monto),
        color = colorTipo(tipo),
        style = style,
    )
}

@Composable
fun SelectorMes(mes: YearMonth, onCambio: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onCambio(mes.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior")
        }
        Text(
            formatoMes(mes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).clickable { onCambio(YearMonth.now()) },
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { onCambio(mes.plusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFecha(inicial: LocalDate, onElegir: (LocalDate) -> Unit, onCerrar: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = inicial.aMillisUtc())
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onElegir(it.aFechaUtc()) }
                onCerrar()
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorColor(seleccionado: Long, onElegir: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        paleta.forEach { c ->
            Box(
                Modifier
                    .size(36.dp)
                    .background(Color(c), CircleShape)
                    .then(
                        if (c == seleccionado) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier
                    )
                    .clickable { onElegir(c) },
                contentAlignment = Alignment.Center,
            ) {
                if (c == seleccionado) Icon(Icons.Filled.Check, null, tint = Color.White)
            }
        }
    }
}
