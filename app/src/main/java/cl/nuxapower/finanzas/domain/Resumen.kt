package cl.nuxapower.finanzas.domain

import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.data.model.Tipo
import java.time.LocalDate
import java.time.YearMonth

data class Parte(
    val id: String,
    val nombre: String,
    val icono: String,
    val color: Long,
    val monto: Long,
    /** Fracción del total (0..1). */
    val fraccion: Float,
    val montoMesAnterior: Long = 0,
)

data class PuntoMes(val mes: YearMonth, val ingresos: Long, val gastos: Long)

data class Resumen(
    val mes: YearMonth,
    val ingresos: Long,
    val gastos: Long,
    val ingresosMesAnterior: Long,
    val gastosMesAnterior: Long,
    val gastoPorCategoria: List<Parte>,
    val gastoPorPersona: List<Parte>,
    val ingresoPorCategoria: List<Parte> = emptyList(),
    /** Últimos 6 meses terminando en [mes], del más antiguo al más reciente. */
    val tendencia: List<PuntoMes> = emptyList(),
    /**
     * Si [mes] es el mes en curso, la comparación usa solo los días 1..corteDia del mes anterior
     * (comparar 6 días contra un mes completo exagera la baja). null = mes completo.
     */
    val corteDia: Int? = null,
) {
    val balance: Long get() = ingresos - gastos
    val balanceMesAnterior: Long get() = ingresosMesAnterior - gastosMesAnterior
}

/** Variación porcentual redondeada; null si el mes anterior fue 0. */
fun variacion(actual: Long, anterior: Long): Int? =
    if (anterior == 0L) null else Math.round((actual - anterior) * 100.0 / anterior).toInt()

fun calcularResumen(
    movimientos: List<Movimiento>,
    categorias: List<Categoria>,
    personas: List<Persona>,
    mes: YearMonth,
    hoy: LocalDate = LocalDate.now(),
): Resumen {
    val corteDia = hoy.dayOfMonth.takeIf { YearMonth.from(hoy) == mes }
    val delMes = movimientos.filter { YearMonth.from(it.fecha) == mes }
    val anterior = movimientos.filter {
        YearMonth.from(it.fecha) == mes.minusMonths(1) && (corteDia == null || it.fecha.dayOfMonth <= corteDia)
    }
    fun List<Movimiento>.total(t: Tipo) = filter { it.tipo == t }.sumOf { it.monto }

    val gastos = delMes.filter { it.tipo == Tipo.GASTO }
    val totalGastos = gastos.sumOf { it.monto }
    val catPorId = categorias.associateBy { it.id }
    val personaPorId = personas.associateBy { it.id }

    fun fraccion(m: Long) = if (totalGastos == 0L) 0f else m.toFloat() / totalGastos

    fun porCategoria(tipo: Tipo): List<Parte> {
        val actuales = delMes.filter { it.tipo == tipo }
        val total = actuales.sumOf { it.monto }
        val previos = anterior.filter { it.tipo == tipo }
            .groupBy { it.categoriaId }.mapValues { (_, l) -> l.sumOf { it.monto } }
        return actuales.groupBy { it.categoriaId }.map { (id, l) ->
            val c = catPorId[id]
            val monto = l.sumOf { it.monto }
            Parte(
                id = id,
                nombre = c?.nombre ?: "Sin categoría",
                icono = c?.icono ?: "❔",
                color = c?.color ?: 0xFF9E9E9E,
                monto = monto,
                fraccion = if (total == 0L) 0f else monto.toFloat() / total,
                montoMesAnterior = previos[id] ?: 0,
            )
        }.sortedByDescending { it.monto }
    }

    val porPersona = gastos.groupBy { it.personaId }.map { (id, l) ->
        val p = personaPorId[id]
        val monto = l.sumOf { it.monto }
        Parte(id, p?.nombre ?: "?", "", p?.color ?: 0xFF9E9E9E, monto, fraccion(monto))
    }.sortedByDescending { it.monto }

    return Resumen(
        mes = mes,
        ingresos = delMes.total(Tipo.INGRESO),
        gastos = totalGastos,
        ingresosMesAnterior = anterior.total(Tipo.INGRESO),
        gastosMesAnterior = anterior.total(Tipo.GASTO),
        gastoPorCategoria = porCategoria(Tipo.GASTO),
        gastoPorPersona = porPersona,
        ingresoPorCategoria = porCategoria(Tipo.INGRESO),
        tendencia = (5 downTo 0).map { i ->
            val m = mes.minusMonths(i.toLong())
            val delM = movimientos.filter { YearMonth.from(it.fecha) == m }
            PuntoMes(m, delM.total(Tipo.INGRESO), delM.total(Tipo.GASTO))
        },
        corteDia = corteDia,
    )
}
