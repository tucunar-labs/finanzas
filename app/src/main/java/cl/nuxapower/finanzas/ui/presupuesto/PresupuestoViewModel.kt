package cl.nuxapower.finanzas.ui.presupuesto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.repo.FiltroSolicitado
import cl.nuxapower.finanzas.data.repo.HogarDatos
import cl.nuxapower.finanzas.data.repo.PresupuestoRepository
import cl.nuxapower.finanzas.data.repo.Solicitudes
import cl.nuxapower.finanzas.domain.EstadoPresupuesto
import cl.nuxapower.finanzas.domain.calcularPresupuesto
import cl.nuxapower.finanzas.domain.presupuestoVigente
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PresupuestoViewModel @Inject constructor(
    private val datos: HogarDatos,
    private val repo: PresupuestoRepository,
    private val solicitudes: Solicitudes,
) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now())
    val mesActual: StateFlow<YearMonth> = mes

    /** null mientras cargan los movimientos. */
    val estado: StateFlow<EstadoPresupuesto?> =
        combine(mes, datos.movimientos, datos.categorias, datos.presupuestos) { m, movs, cats, pres ->
            movs?.let { calcularPresupuesto(it, cats, pres, m) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun mes(m: YearMonth) {
        mes.value = m
    }

    /**
     * Define el monto de una categoría para el mes mostrado (0 = quitar). Si el mes heredaba
     * montos de uno anterior, se copian para que este mes quede completo y el anterior intacto.
     */
    fun definir(categoriaId: String, monto: Long) {
        val hid = datos.hogarId ?: return
        val m = mes.value
        val (_, vigentes) = presupuestoVigente(datos.presupuestos.value, m)
        repo.guardar(hid, m, vigentes + (categoriaId to monto))
    }

    fun verMovimientos(categoriaId: String) {
        solicitudes.filtroMovimientos.value = FiltroSolicitado(mes.value, categoriaId, tipo = Tipo.GASTO)
    }
}
