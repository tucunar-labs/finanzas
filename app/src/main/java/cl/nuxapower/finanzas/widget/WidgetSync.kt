package cl.nuxapower.finanzas.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.repo.HogarDatos
import cl.nuxapower.finanzas.data.repo.Sesion
import cl.nuxapower.finanzas.data.repo.SesionRepository
import cl.nuxapower.finanzas.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Mantiene el widget al día con las categorías de gasto activas y el estado de la sesión. */
@Singleton
class WidgetSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sesion: SesionRepository,
    private val datos: HogarDatos,
    @AppScope private val scope: CoroutineScope,
) {
    fun iniciar() {
        scope.launch {
            combine(sesion.sesion, datos.categorias) { s, cats -> s to cats }
                // Mientras carga la sesión o llegan las categorías no se toca lo que ya muestra el widget.
                .filter { (s, cats) -> s !is Sesion.Cargando && (s !is Sesion.ConHogar || cats.isNotEmpty()) }
                .map { (s, cats) ->
                    if (s !is Sesion.ConHogar) null
                    else cats.filter { it.tipo == Tipo.GASTO && !it.archivada }
                        .sortedBy { it.orden }
                        .map { CategoriaWidget(it.id, it.icono, it.nombre) }
                }
                .distinctUntilChanged()
                .collect {
                    WidgetCache.guardar(context, it)
                    runCatching { GastoRapidoWidget().updateAll(context) }
                }
        }
    }
}
