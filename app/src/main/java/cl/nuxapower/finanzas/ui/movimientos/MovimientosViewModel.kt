package cl.nuxapower.finanzas.ui.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.repo.HogarDatos
import cl.nuxapower.finanzas.data.repo.MovimientoRepository
import cl.nuxapower.finanzas.data.repo.Solicitudes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class Filtros(
    val mes: YearMonth = YearMonth.now(),
    val tipo: Tipo? = null,
    val categoriaId: String? = null,
    val personaId: String? = null,
    /** Texto libre: busca en nota, categoría y persona, en todos los meses. */
    val busqueda: String = "",
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
    val pendientes: Int = 0,
)

@HiltViewModel
class MovimientosViewModel @Inject constructor(
    private val datos: HogarDatos,
    private val repo: MovimientoRepository,
    solicitudes: Solicitudes,
) : ViewModel() {

    private val filtros = MutableStateFlow(Filtros())

    init {
        // Filtro pedido desde otra pantalla (ej. tocar una categoría en el Resumen).
        viewModelScope.launch {
            solicitudes.filtroMovimientos.filterNotNull().collect { s ->
                filtros.value = Filtros(mes = s.mes, tipo = s.tipo, categoriaId = s.categoriaId, personaId = s.personaId)
                solicitudes.filtroMovimientos.value = null
            }
        }
    }

    val estado: StateFlow<MovimientosEstado> =
        combine(filtros, datos.movimientos, datos.categorias, datos.personas, datos.pendientes) { f, movs, cats, pers, pend ->
            val catMap = cats.associateBy { it.id }
            val perMap = pers.associateBy { it.id }
            val q = normalizar(f.busqueda)
            val filtrados = movs.orEmpty().filter { m ->
                (if (q.isEmpty()) YearMonth.from(m.fecha) == f.mes else coincide(m, q, catMap, perMap)) &&
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
                categorias = catMap,
                personas = perMap,
                categoriasFiltro = cats.filter { f.tipo == null || it.tipo == f.tipo },
                pendientes = pend,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovimientosEstado())

    fun mes(m: YearMonth) = filtros.update { it.copy(mes = m) }
    fun buscar(texto: String) = filtros.update { it.copy(busqueda = texto) }
    fun limpiarFiltros() = filtros.update { Filtros(mes = it.mes) }
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

/** Minúsculas y sin tildes, para que "credito" encuentre "Crédito". */
private fun normalizar(s: String): String =
    java.text.Normalizer.normalize(s.trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

private fun coincide(m: Movimiento, q: String, cats: Map<String, Categoria>, pers: Map<String, Persona>): Boolean =
    listOf(m.nota, cats[m.categoriaId]?.nombre.orEmpty(), pers[m.personaId]?.nombre.orEmpty(), m.monto.toString())
        .any { normalizar(it).contains(q) }
