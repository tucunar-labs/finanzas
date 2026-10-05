package cl.nuxapower.finanzas.ui.resumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.repo.HogarDatos
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
class ResumenViewModel @Inject constructor(datos: HogarDatos) : ViewModel() {

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
}
