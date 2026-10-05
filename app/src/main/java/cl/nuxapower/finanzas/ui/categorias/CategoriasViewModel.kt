package cl.nuxapower.finanzas.ui.categorias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.repo.CategoriaRepository
import cl.nuxapower.finanzas.data.repo.HogarDatos
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CategoriasEstado(
    val tipo: Tipo = Tipo.GASTO,
    val activas: List<Categoria> = emptyList(),
    val archivadas: List<Categoria> = emptyList(),
)

@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val datos: HogarDatos,
    private val repo: CategoriaRepository,
) : ViewModel() {

    private val tipo = MutableStateFlow(Tipo.GASTO)

    val estado: StateFlow<CategoriasEstado> = combine(tipo, datos.categorias) { t, cats ->
        val delTipo = cats.filter { it.tipo == t }.sortedBy { it.orden }
        CategoriasEstado(t, delTipo.filterNot { it.archivada }, delTipo.filter { it.archivada })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriasEstado())

    fun tipo(t: Tipo) {
        tipo.value = t
    }

    fun guardar(c: Categoria) {
        val hid = datos.hogarId ?: return
        val nueva = if (c.id.isEmpty()) {
            c.copy(orden = (datos.categorias.value.maxOfOrNull { it.orden } ?: -1) + 1)
        } else c
        repo.guardar(hid, nueva.copy(nombre = nueva.nombre.trim()))
    }

    fun archivar(c: Categoria, archivada: Boolean) {
        datos.hogarId?.let { repo.archivar(it, c.id, archivada) }
    }

    /** Mueve una categoría activa una posición arriba (-1) o abajo (+1) dentro de su tipo. */
    fun mover(c: Categoria, delta: Int) {
        val hid = datos.hogarId ?: return
        val lista = estado.value.activas
        val i = lista.indexOfFirst { it.id == c.id }
        val j = i + delta
        if (i < 0 || j !in lista.indices) return
        repo.intercambiar(hid, lista[i], lista[j])
    }
}
