package cl.nuxapower.finanzas.domain

import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.Tipo
import java.time.YearMonth

data class LineaPresupuesto(
    val categoria: Categoria,
    /** 0 = sin presupuesto. */
    val presupuesto: Long,
    val gastado: Long,
) {
    val tienePresupuesto: Boolean get() = presupuesto > 0
    val fraccion: Float get() = if (presupuesto > 0) gastado.toFloat() / presupuesto else 0f
    val excedido: Boolean get() = tienePresupuesto && gastado > presupuesto
    val restante: Long get() = presupuesto - gastado
}

data class EstadoPresupuesto(
    val mes: YearMonth,
    /** Mes del que se toman los montos (null si nunca se definió uno). Distinto de [mes] si se hereda. */
    val origen: YearMonth?,
    val lineas: List<LineaPresupuesto>,
) {
    val conPresupuesto: List<LineaPresupuesto> get() = lineas.filter { it.tienePresupuesto }
    val sinPresupuesto: List<LineaPresupuesto> get() = lineas.filterNot { it.tienePresupuesto }
    val totalPresupuesto: Long get() = conPresupuesto.sumOf { it.presupuesto }
    /** Gasto en categorías con presupuesto (para comparar peras con peras). */
    val totalGastadoPresupuestado: Long get() = conPresupuesto.sumOf { it.gastado }
    val excedidas: Int get() = conPresupuesto.count { it.excedido }
}

/** Presupuesto vigente de [mes]: el propio o, si no existe, el del último mes anterior que tenga. */
fun presupuestoVigente(porMes: Map<YearMonth, Map<String, Long>>, mes: YearMonth): Pair<YearMonth?, Map<String, Long>> {
    val origen = porMes.keys.filter { it <= mes }.maxOrNull() ?: return null to emptyMap()
    return origen to porMes.getValue(origen)
}

fun calcularPresupuesto(
    movimientos: List<Movimiento>,
    categorias: List<Categoria>,
    porMes: Map<YearMonth, Map<String, Long>>,
    mes: YearMonth,
): EstadoPresupuesto {
    val (origen, montos) = presupuestoVigente(porMes, mes)
    val gastado = movimientos
        .filter { it.tipo == Tipo.GASTO && YearMonth.from(it.fecha) == mes }
        .groupBy { it.categoriaId }
        .mapValues { (_, l) -> l.sumOf { it.monto } }
    val lineas = categorias
        .filter { it.tipo == Tipo.GASTO }
        // Las archivadas solo aparecen si tienen presupuesto o gasto este mes.
        .filter { !it.archivada || (montos[it.id] ?: 0) > 0 || (gastado[it.id] ?: 0) > 0 }
        .sortedBy { it.orden }
        .map { LineaPresupuesto(it, montos[it.id] ?: 0, gastado[it.id] ?: 0) }
    return EstadoPresupuesto(mes, origen, lineas)
}
