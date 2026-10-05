package cl.nuxapower.finanzas.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.repo.HogarDatos
import cl.nuxapower.finanzas.data.repo.Sesion
import cl.nuxapower.finanzas.data.repo.SesionRepository
import cl.nuxapower.finanzas.di.AppScope
import cl.nuxapower.finanzas.domain.calcularPresupuesto
import cl.nuxapower.finanzas.util.capitalizar
import cl.nuxapower.finanzas.util.localeCl
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Mantiene el widget al día: categorías de gasto activas, gasto del mes y presupuesto restante. */
@Singleton
class WidgetSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sesion: SesionRepository,
    private val datos: HogarDatos,
    @AppScope private val scope: CoroutineScope,
) {
    private val formatoMes = DateTimeFormatter.ofPattern("MMMM", localeCl)

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun iniciar() {
        scope.launch {
            combine(sesion.sesion, datos.categorias, datos.movimientos, datos.presupuestos) { s, cats, movs, pres ->
                Entrada(s, cats, movs, pres)
            }
                // Mientras carga la sesión o los datos no se toca lo que ya muestra el widget.
                .filter { e -> e.sesion !is Sesion.Cargando && (e.sesion !is Sesion.ConHogar || (e.cats.isNotEmpty() && e.movs != null)) }
                .map { e ->
                    if (e.sesion !is Sesion.ConHogar) return@map null
                    val mes = YearMonth.now()
                    val presupuesto = calcularPresupuesto(e.movs.orEmpty(), e.cats, e.pres, mes)
                    DatosWidget(
                        categorias = e.cats.filter { it.tipo == Tipo.GASTO && !it.archivada }
                            .sortedBy { it.orden }
                            .map { CategoriaWidget(it.id, it.icono, it.nombre) },
                        mes = mes.format(formatoMes).capitalizar(),
                        gastado = e.movs.orEmpty()
                            .filter { it.tipo == Tipo.GASTO && YearMonth.from(it.fecha) == mes }
                            .sumOf { it.monto },
                        restante = presupuesto.totalPresupuesto.takeIf { it > 0 }
                            ?.let { it - presupuesto.totalGastadoPresupuestado },
                    )
                }
                .distinctUntilChanged()
                .debounce(500)
                .collect {
                    WidgetCache.guardar(context, it)
                    runCatching { GastoRapidoWidget().updateAll(context) }
                }
        }
    }

    private data class Entrada(
        val sesion: Sesion,
        val cats: List<cl.nuxapower.finanzas.data.model.Categoria>,
        val movs: List<cl.nuxapower.finanzas.data.model.Movimiento>?,
        val pres: Map<YearMonth, Map<String, Long>>,
    )
}
