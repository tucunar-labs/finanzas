package cl.nuxapower.finanzas.ui.ingreso

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.repo.HogarDatos
import cl.nuxapower.finanzas.data.repo.MovimientoRepository
import cl.nuxapower.finanzas.data.repo.Solicitudes
import cl.nuxapower.finanzas.util.formatoClp
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_DIGITOS = 12

data class FormMovimiento(
    val tipo: Tipo = Tipo.GASTO,
    val digitos: String = "",
    /** Montos ya sumados con "+" (para sumar varias boletas en un solo movimiento). */
    val sumandos: List<Long> = emptyList(),
    val categoriaId: String? = null,
    val personaId: String? = null,
    val fecha: LocalDate = LocalDate.now(),
    val nota: String = "",
    val error: String? = null,
    /** Movimiento original cuando se está editando. */
    val original: Movimiento? = null,
) {
    val monto: Long get() = sumandos.sum() + (digitos.toLongOrNull() ?: 0L)
}

data class IngresoEstado(
    val form: FormMovimiento = FormMovimiento(),
    /** Categorías elegibles: activas del tipo actual (+ la ya asignada si está archivada). */
    val categorias: List<Categoria> = emptyList(),
    val personas: List<Persona> = emptyList(),
    val editando: Boolean = false,
    /** Notas usadas antes en la categoría elegida (o en general), para no reescribirlas. */
    val sugerencias: List<String> = emptyList(),
)

sealed interface EventoIngreso {
    data class Guardado(val id: String, val mensaje: String) : EventoIngreso
    data object Editado : EventoIngreso
    data class Eliminado(val movimiento: Movimiento) : EventoIngreso
    data object NoEncontrado : EventoIngreso
    data class Duplicado(val id: String) : EventoIngreso
}

/** Sirve para el ingreso rápido y para editar un movimiento existente (argumento de navegación "id"). */
@HiltViewModel
class IngresoViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val datos: HogarDatos,
    private val repo: MovimientoRepository,
    private val solicitudes: Solicitudes,
) : ViewModel() {

    private val editandoId: String? = savedState["id"]
    // Categoría preelegida desde el widget de inicio.
    private val form = MutableStateFlow(FormMovimiento(personaId = datos.uid, categoriaId = savedState["categoriaId"]))

    private val _eventos = Channel<EventoIngreso>(Channel.BUFFERED)
    val eventos: Flow<EventoIngreso> = _eventos.receiveAsFlow()

    val estado: StateFlow<IngresoEstado> =
        combine(form, datos.categorias, datos.personas, datos.movimientos) { f, cats, pers, movs ->
            IngresoEstado(
                form = f,
                categorias = cats.filter { it.tipo == f.tipo && (!it.archivada || it.id == f.categoriaId) },
                personas = pers,
                editando = editandoId != null,
                sugerencias = sugerenciasNota(movs.orEmpty(), f),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IngresoEstado(editando = editandoId != null))

    init {
        // Atajo del launcher "Nuevo ingreso/gasto".
        if (editandoId == null) viewModelScope.launch {
            solicitudes.tipoIngreso.collect { t ->
                if (t != null) {
                    tipo(t)
                    solicitudes.tipoIngreso.value = null
                }
            }
        }
        if (editandoId != null) viewModelScope.launch {
            val m = datos.movimientos.filterNotNull().first().find { it.id == editandoId }
            if (m == null) {
                _eventos.send(EventoIngreso.NoEncontrado)
            } else {
                form.value = FormMovimiento(
                    tipo = m.tipo,
                    digitos = m.monto.toString(),
                    categoriaId = m.categoriaId,
                    personaId = m.personaId,
                    fecha = m.fecha,
                    nota = m.nota,
                    original = m,
                )
            }
        }
    }

    fun tecla(t: String) = form.update {
        val nuevo = (it.digitos + t).trimStart('0')
        if (nuevo.length > MAX_DIGITOS) it else it.copy(digitos = nuevo, error = null)
    }

    /** "+": guarda el número actual como sumando y empieza otro. */
    fun sumar() = form.update {
        val actual = it.digitos.toLongOrNull() ?: 0L
        if (actual == 0L) it else it.copy(sumandos = it.sumandos + actual, digitos = "")
    }

    /** Borra un dígito; si no quedan, recupera el último sumando para seguir editándolo. */
    fun borrar() = form.update {
        when {
            it.digitos.isNotEmpty() -> it.copy(digitos = it.digitos.dropLast(1))
            it.sumandos.isNotEmpty() -> it.copy(digitos = it.sumandos.last().toString(), sumandos = it.sumandos.dropLast(1))
            else -> it
        }
    }

    fun limpiarMonto() = form.update { it.copy(digitos = "", sumandos = emptyList()) }

    fun tipo(t: Tipo) = form.update {
        if (it.tipo == t) it else it.copy(tipo = t, categoriaId = null, error = null)
    }

    fun categoria(id: String) = form.update { it.copy(categoriaId = id, error = null) }
    fun persona(id: String) = form.update { it.copy(personaId = id, error = null) }
    fun fecha(d: LocalDate) = form.update { it.copy(fecha = d) }
    fun nota(s: String) = form.update { it.copy(nota = s.take(500)) }

    fun guardar() {
        // Si el ViewModel nació antes de cargar la sesión (widget en frío), la persona por defecto es quien usa el teléfono.
        val f = form.value.let { if (it.personaId == null) it.copy(personaId = datos.uid) else it }
        val hid = datos.hogarId ?: return
        val error = when {
            f.monto <= 0 -> "Ingresa un monto mayor a 0"
            f.categoriaId == null -> "Elige una categoría"
            f.personaId == null -> "Elige una persona"
            else -> null
        }
        if (error != null) {
            form.update { it.copy(error = error) }
            return
        }
        val base = Movimiento(
            id = f.original?.id.orEmpty(),
            tipo = f.tipo,
            monto = f.monto,
            categoriaId = f.categoriaId!!,
            personaId = f.personaId!!,
            fecha = f.fecha,
            nota = f.nota.trim(),
            creadoPor = f.original?.creadoPor ?: datos.uid.orEmpty(),
            creadoEn = f.original?.creadoEn,
        )
        if (f.original != null) {
            repo.editar(hid, base)
            _eventos.trySend(EventoIngreso.Editado)
        } else {
            val id = repo.crear(hid, base)
            val cat = estado.value.categorias.find { it.id == base.categoriaId }?.nombre.orEmpty()
            val tipoTxt = if (base.tipo == Tipo.GASTO) "Gasto" else "Ingreso"
            _eventos.trySend(EventoIngreso.Guardado(id, "$tipoTxt de ${formatoClp(base.monto)} en $cat"))
            // Limpia el formulario; mantiene tipo y persona para cargar varios seguidos.
            form.value = FormMovimiento(tipo = f.tipo, personaId = f.personaId)
        }
    }

    /** Crea una copia con fecha de hoy (gastos que se repiten: arriendo, cuentas, etc.). */
    fun duplicar() {
        val m = form.value.original ?: return
        val hid = datos.hogarId ?: return
        val id = repo.crear(hid, m.copy(fecha = LocalDate.now(), creadoPor = datos.uid.orEmpty()))
        _eventos.trySend(EventoIngreso.Duplicado(id))
    }

    fun eliminar() {
        val m = form.value.original ?: return
        val hid = datos.hogarId ?: return
        repo.eliminar(hid, m.id)
        _eventos.trySend(EventoIngreso.Eliminado(m))
    }

    // Deshacer: no usan viewModelScope, así funcionan aunque la pantalla ya se haya cerrado.
    fun deshacerCreacion(id: String) {
        datos.hogarId?.let { repo.eliminar(it, id) }
    }

    fun restaurar(m: Movimiento) {
        datos.hogarId?.let { repo.restaurar(it, m) }
    }
}

/** Hasta 4 notas recientes distintas, de la categoría elegida si hay una. */
private fun sugerenciasNota(movs: List<Movimiento>, f: FormMovimiento): List<String> =
    movs.asSequence()
        .filter { it.tipo == f.tipo && it.nota.isNotBlank() }
        .filter { f.categoriaId == null || it.categoriaId == f.categoriaId }
        .map { it.nota.trim() }
        .filter { it != f.nota.trim() }
        .distinctBy { it.lowercase() }
        .take(4)
        .toList()
