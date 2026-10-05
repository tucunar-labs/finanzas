package cl.nuxapower.finanzas.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import cl.nuxapower.finanzas.MainActivity
import cl.nuxapower.finanzas.util.formatoClp

/** Clave del extra con la categoría preelegida al abrir [GastoRapidoActivity]. */
val CATEGORIA_ID = ActionParameters.Key<String>("categoriaId")

// Medidas de la grilla: celdas de tamaño fijo, tantas como quepan en el widget.
private val ANCHO_CELDA = 70.dp
private val ALTO_CELDA = 56.dp
private val ALTO_ENCABEZADO = 52.dp
private val MARGEN = 10.dp

/**
 * Widget de inicio "Gasto rápido": encabezado con lo gastado en el mes (y lo que queda del
 * presupuesto), botón "+ Gasto" y una grilla con las categorías. Tocar una categoría abre el
 * teclado con ella elegida; la persona (quien usa el teléfono) y la fecha (hoy) se ponen solas.
 */
class GastoRapidoWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val datos = WidgetCache.leer(context)
        provideContent { GlanceTheme { Contenido(datos) } }
    }
}

class GastoRapidoReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = GastoRapidoWidget()
}

@Composable
private fun Contenido(datos: DatosWidget?) {
    val size = LocalSize.current
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(24.dp)
            .background(GlanceTheme.colors.widgetBackground).padding(MARGEN),
    ) {
        Encabezado(datos)
        if (datos == null) {
            Text(
                "Abre Finanzas para comenzar",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                modifier = GlanceModifier.padding(top = 8.dp, start = 4.dp).clickable(actionStartActivity<MainActivity>()),
            )
        } else {
            Spacer(GlanceModifier.height(6.dp))
            Grilla(datos.categorias, size)
        }
    }
}

@Composable
private fun Encabezado(datos: DatosWidget?) {
    Row(GlanceModifier.fillMaxWidth().height(ALTO_ENCABEZADO), verticalAlignment = Alignment.CenterVertically) {
        Column(GlanceModifier.defaultWeight().padding(start = 4.dp)) {
            Text(
                "Gasto rápido",
                maxLines = 1,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GlanceTheme.colors.onSurface),
            )
            if (datos != null) {
                val detalle = buildString {
                    append(datos.mes).append(": ").append(formatoClp(datos.gastado))
                    datos.restante?.let {
                        append(if (it >= 0) " · quedan ${formatoClp(it)}" else " · pasados ${formatoClp(-it)}")
                    }
                }
                Text(
                    detalle,
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = if ((datos.restante ?: 0) < 0) ColorProvider(androidx.compose.ui.graphics.Color(0xFFE5534B))
                        else GlanceTheme.colors.onSurfaceVariant,
                    ),
                )
            }
        }
        // Botón principal: abre el teclado sin categoría elegida.
        Box(
            GlanceModifier.cornerRadius(18.dp).background(GlanceTheme.colors.primary)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clickable(actionStartActivity<GastoRapidoActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "+ Gasto",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GlanceTheme.colors.onPrimary),
            )
        }
    }
}

@Composable
private fun Grilla(categorias: List<CategoriaWidget>, size: DpSize) {
    val ancho = size.width - MARGEN * 2
    val alto = size.height - MARGEN * 2 - ALTO_ENCABEZADO - 6.dp
    val columnas = (ancho / ANCHO_CELDA).toInt().coerceIn(2, 6)
    val filas = (alto / ALTO_CELDA).toInt().coerceAtLeast(1)
    categorias.take(columnas * filas).chunked(columnas).forEach { fila ->
        Row(GlanceModifier.fillMaxWidth().height(ALTO_CELDA)) {
            fila.forEach { c -> Celda(c) }
            // Relleno para que la última fila mantenga el ancho de las celdas.
            repeat(columnas - fila.size) { Spacer(GlanceModifier.defaultWeight()) }
        }
    }
}

@Composable
private fun androidx.glance.layout.RowScope.Celda(c: CategoriaWidget) {
    // Ojo: fillMaxSize/fillMaxWidth anulan el peso en Glance y la celda se come la fila.
    Box(GlanceModifier.defaultWeight().height(ALTO_CELDA).padding(3.dp)) {
        Column(
            GlanceModifier.fillMaxSize().cornerRadius(14.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .clickable(actionStartActivity<GastoRapidoActivity>(actionParametersOf(CATEGORIA_ID to c.id))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(c.icono, maxLines = 1, style = TextStyle(fontSize = 18.sp))
            Text(
                c.nombre,
                maxLines = 1,
                style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSecondaryContainer),
                modifier = GlanceModifier.padding(horizontal = 4.dp),
            )
        }
    }
}
