package cl.nuxapower.finanzas.data.repo

import cl.nuxapower.finanzas.data.model.Tipo
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

/** Filtro que una pantalla pide aplicar en Movimientos (ej. tocar una categoría en el Resumen). */
data class FiltroSolicitado(val mes: YearMonth, val categoriaId: String? = null, val personaId: String? = null, val tipo: Tipo? = null)

/**
 * Pedidos de navegación entre pantallas que no caben en una ruta: el atajo del launcher
 * ("Nuevo ingreso") y el salto desde el Resumen a Movimientos filtrados. Quien los consume los limpia.
 */
@Singleton
class Solicitudes @Inject constructor() {
    val tipoIngreso = MutableStateFlow<Tipo?>(null)
    val filtroMovimientos = MutableStateFlow<FiltroSolicitado?>(null)
}
