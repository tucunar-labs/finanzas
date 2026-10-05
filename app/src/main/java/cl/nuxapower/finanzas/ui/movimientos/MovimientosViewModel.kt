package cl.nuxapower.finanzas.ui.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.repo.HogarDatos
import cl.nuxapower.finanzas.data.repo.MovimientoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class Filtros(
    val mes: YearMonth = YearMonth.now(),
    val tipo: Tipo? = null,
    val categoriaId: String? = null,
    val personaId: String? = null,
)

data class Dia(val fecha: LocalDate, val movimientos: List<Movimiento>, val neto: Long)

data class MovimientosEstado(
    val cargando: Boolean = true,
    val filtros: Filtros = Filtros(),
    val dias: List<Dia> = emptyList(),
    val totalIngresos: Long = 0,
    val totalGastos: Long = 0,
    val categorias: Map<String, Categoria> = emptyMap(),
    val personas: Map<String, Persona> = emptyMap(),
    /** Categorías para el filtro (incluye archivadas: siguen en el historial). */
    val categoriasFiltro: List<Categoria> = emptyList(),
)

@HiltViewModel
class MovimientosViewModel @Inject constructor(
    private val datos: HogarDatos,
    private val repo: MovimientoRepository,
) : ViewModel() {

    private val filtros = MutableStateFlow(Filtros())

    val estado: StateFlow<MovimientosEstado> =
        combine(filtros, datos.movimientos, datos.categorias, datos.personas) { f, movs, cats, pers ->
            val filtrados = movs.orEmpty().filter { m ->
                YearMonth.from(m.fecha) == f.mes &&
                    (f.tipo == null || m.tipo == f.tipo) &&
                    (f.categoriaId == null || m.categoriaId == f.categoriaId) &&
                    (f.personaId == null || m.personaId == f.personaId)
            }
            MovimientosEstado(
                cargando = movs == null,
                filtros = f,
                dias = filtrados.groupBy { it.fecha }.map { (fecha, lista) ->
                    Dia(fecha, lista, lista.sumOf { if (it.tipo == Tipo.INGRESO) it.monto else -it.monto })
                },
                totalIngresos = filtrados.filter { it.tipo == Tipo.INGRESO }.sumOf { it.monto },
                totalGastos = filtrados.filter { it.tipo == Tipo.GASTO }.sumOf { it.monto },
                categorias = cats.associateBy { it.id },
                personas = pers.associateBy { it.id },
                categoriasFiltro = cats.filter { f.tipo == null || it.tipo == f.tipo },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovimientosEstado())

    fun mes(m: YearMonth) = filtros.update { it.copy(mes = m) }
    fun tipo(t: Tipo?) = filtros.update { f ->
        // Si la categoría elegida no es del nuevo tipo, se quita del filtro.
        val cat = datos.categorias.value.find { it.id == f.categoriaId }
        f.copy(tipo = t, categoriaId = f.categoriaId.takeIf { t == null || cat?.tipo == t })
    }
    fun categoria(id: String?) = filtros.update { it.copy(categoriaId = id) }
    fun persona(id: String?) = filtros.update { it.copy(personaId = id) }

    fun eliminar(m: Movimiento) {
        datos.hogarId?.let { repo.eliminar(it, m.id) }
    }

    fun restaurar(m: Movimiento) {
        datos.hogarId?.let { repo.restaurar(it, m) }
    }
}
