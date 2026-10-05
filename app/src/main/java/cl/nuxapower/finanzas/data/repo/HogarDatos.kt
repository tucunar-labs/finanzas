package cl.nuxapower.finanzas.data.repo

import android.util.Log
import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Hogar
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.di.AppScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Datos del hogar actual con listeners en tiempo real, compartidos por todas las pantallas.
 * Un hogar de dos personas genera pocos movimientos, así que se escucha la colección completa
 * y los filtros y resúmenes se calculan en memoria.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class HogarDatos @Inject constructor(
    private val sesionRepo: SesionRepository,
    hogares: HogarRepository,
    movimientosRepo: MovimientoRepository,
    @AppScope private val scope: CoroutineScope,
) {
    private val hid: Flow<String?> = sesionRepo.sesion
        .map { (it as? Sesion.ConHogar)?.hogarId }
        .distinctUntilChanged()

    /** Hogar actual, o null si no hay sesión con hogar. */
    val hogarId: String? get() = (sesionRepo.sesion.value as? Sesion.ConHogar)?.hogarId
    val uid: String? get() = (sesionRepo.sesion.value as? Sesion.ConHogar)?.uid

    val hogar: StateFlow<Hogar?> = porHogar(null) { hogares.hogar(it) }
    val personas: StateFlow<List<Persona>> = porHogar(emptyList()) { hogares.personas(it) }
    val categorias: StateFlow<List<Categoria>> = porHogar(emptyList()) { hogares.categorias(it) }

    /** null mientras carga la primera vez. Ordenados del más reciente al más antiguo. */
    val movimientos: StateFlow<List<Movimiento>?> = porHogar(null) { id ->
        movimientosRepo.movimientos(id).map { lista ->
            lista.sortedWith(
                compareByDescending<Movimiento> { it.fecha }
                    .thenByDescending { it.creadoEn ?: java.time.Instant.MAX }
            )
        }
    }

    /** Movimientos guardados localmente que aún no llegan al servidor. */
    val pendientes: StateFlow<Int> = movimientos
        .map { l -> l.orEmpty().count { it.pendiente } }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    private fun <T> porHogar(inicial: T, fuente: (String) -> Flow<T>): StateFlow<T> =
        hid.flatMapLatest { id ->
            if (id == null) flowOf(inicial)
            else fuente(id).catch { e ->
                // Pasa al cerrar sesión (el listener pierde permiso): no es un error para el usuario.
                Log.w("HogarDatos", "Listener cerrado", e)
                emit(inicial)
            }
        }.stateIn(scope, SharingStarted.Eagerly, inicial)
}
