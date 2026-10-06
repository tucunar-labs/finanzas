package cl.nuxapower.finanzas.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxHeight
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

// Grilla fija de 2×2 con celdas grandes: las 4 primeras categorías según su orden (se cambia en
// Ajustes → Categorías); el resto queda en "+ Gasto". Las filas se estiran para llenar el widget. No se usa LocalSize porque
// el launcher de Xiaomi informa un alto menor al real y quedaba espacio vacío.
// Ese launcher además escala el widget a ~60 %, por eso los textos son grandes en sp.
private const val COLUMNAS = 2
private const val FILAS = 2
private val ALTO_ENCABEZADO = 88.dp
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
            Spacer(GlanceModifier.height(4.dp))
            Grilla(datos.categorias)
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
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, color = GlanceTheme.colors.onSurface),
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
                        fontSize = 22.sp,
                        color = if ((datos.restante ?: 0) < 0) ColorProvider(androidx.compose.ui.graphics.Color(0xFFE5534B))
                        else GlanceTheme.colors.onSurfaceVariant,
                    ),
                )
            }
        }
        // Botón principal: abre el teclado sin categoría elegida.
        Box(
            GlanceModifier.cornerRadius(28.dp).background(GlanceTheme.colors.primary)
                .padding(horizontal = 26.dp, vertical = 16.dp)
                .clickable(actionStartActivity<GastoRapidoActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "+ Gasto",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, color = GlanceTheme.colors.onPrimary),
            )
        }
    }
}

@Composable
private fun ColumnScope.Grilla(categorias: List<CategoriaWidget>) {
    categorias.take(COLUMNAS * FILAS).chunked(COLUMNAS).forEach { fila ->
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            fila.forEach { c -> Celda(c) }
            // Relleno para que la última fila mantenga el ancho de las celdas.
            repeat(COLUMNAS - fila.size) { Spacer(GlanceModifier.defaultWeight()) }
        }
    }
}

@Composable
private fun RowScope.Celda(c: CategoriaWidget) {
    // Ojo: fillMaxSize/fillMaxWidth anulan el peso en Glance y la celda se come la fila.
    Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(6.dp)) {
        Column(
            GlanceModifier.fillMaxSize().cornerRadius(28.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .clickable(actionStartActivity<GastoRapidoActivity>(actionParametersOf(CATEGORIA_ID to c.id))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(c.icono, maxLines = 1, style = TextStyle(fontSize = 34.sp))
            // Nombre grande (el launcher de Xiaomi achica el widget) para leerlo bien en el teléfono.
            Text(
                c.nombre,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSecondaryContainer,
                ),
                modifier = GlanceModifier.padding(horizontal = 8.dp),
            )
        }
    }
}
