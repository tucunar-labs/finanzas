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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import cl.nuxapower.finanzas.MainActivity

/** Clave del extra con la categoría preelegida al abrir [GastoRapidoActivity]. */
val CATEGORIA_ID = ActionParameters.Key<String>("categoriaId")

/**
 * Widget de inicio: una grilla con las categorías de gasto. Tocar una abre el teclado con esa
 * categoría elegida; la persona (quien usa el teléfono) y la fecha (hoy) se ponen solas.
 */
class GastoRapidoWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val categorias = WidgetCache.leer(context)
        provideContent { GlanceTheme { Contenido(categorias) } }
    }
}

class GastoRapidoReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = GastoRapidoWidget()
}

private const val COLUMNAS = 4
private const val MAX_CATEGORIAS = 8

@Composable
private fun Contenido(categorias: List<CategoriaWidget>?) {
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(24.dp)
            .background(GlanceTheme.colors.widgetBackground).padding(10.dp),
    ) {
        Row(
            GlanceModifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
                .clickable(actionStartActivity<GastoRapidoActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Gasto rápido",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text("＋", style = TextStyle(fontSize = 18.sp, color = GlanceTheme.colors.primary))
        }

        if (categorias == null) {
            Text(
                "Abre Finanzas para comenzar",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.padding(6.dp).clickable(actionStartActivity<MainActivity>()),
            )
            return@Column
        }

        categorias.take(MAX_CATEGORIAS).chunked(COLUMNAS).forEach { fila ->
            Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                fila.forEach { c ->
                    Column(
                        GlanceModifier.defaultWeight().fillMaxSize().padding(3.dp),
                    ) {
                        Column(
                            GlanceModifier.fillMaxSize().cornerRadius(16.dp)
                                .background(GlanceTheme.colors.secondaryContainer)
                                .clickable(actionStartActivity<GastoRapidoActivity>(actionParametersOf(CATEGORIA_ID to c.id))),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(c.icono, style = TextStyle(fontSize = 22.sp))
                            Text(
                                c.nombre,
                                maxLines = 1,
                                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSecondaryContainer),
                            )
                        }
                    }
                }
                repeat(COLUMNAS - fila.size) { Spacer(GlanceModifier.defaultWeight()) }
            }
        }
    }
}
