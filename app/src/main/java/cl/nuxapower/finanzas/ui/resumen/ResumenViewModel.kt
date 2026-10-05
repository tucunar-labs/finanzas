package cl.nuxapower.finanzas.ui.resumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.repo.FiltroSolicitado
import cl.nuxapower.finanzas.data.repo.HogarDatos
import cl.nuxapower.finanzas.data.repo.Solicitudes
import cl.nuxapower.finanzas.domain.Resumen
import cl.nuxapower.finanzas.domain.calcularResumen
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ResumenViewModel @Inject constructor(
    datos: HogarDatos,
    private val solicitudes: Solicitudes,
) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now())

    /** null mientras cargan los movimientos. */
    val resumen: StateFlow<Resumen?> =
        combine(mes, datos.movimientos, datos.categorias, datos.personas) { m, movs, cats, pers ->
            movs?.let { calcularResumen(it, cats, pers, m) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val mesActual: StateFlow<YearMonth> = mes

    fun mes(m: YearMonth) {
        mes.value = m
    }

    /** Pide a Movimientos mostrar lo que compone una barra del resumen. */
    fun verMovimientos(tipo: Tipo, categoriaId: String? = null, personaId: String? = null) {
        solicitudes.filtroMovimientos.value = FiltroSolicitado(mes.value, categoriaId, personaId, tipo)
    }
}
