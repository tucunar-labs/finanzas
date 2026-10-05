package cl.nuxapower.finanzas

import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.domain.calcularPresupuesto
import cl.nuxapower.finanzas.domain.presupuestoVigente
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresupuestoTest {
    private val sep = YearMonth.of(2026, 9)
    private val oct = YearMonth.of(2026, 10)
    private val nov = YearMonth.of(2026, 11)

    private val cats = listOf(
        Categoria("super", "Supermercado", "🛒", 1, Tipo.GASTO, orden = 0),
        Categoria("ocio", "Ocio", "🎉", 2, Tipo.GASTO, orden = 1),
        Categoria("viejo", "Viejo", "📦", 3, Tipo.GASTO, archivada = true, orden = 2),
        Categoria("sueldo", "Sueldo", "💼", 4, Tipo.INGRESO, orden = 3),
    )

    private fun gasto(cat: String, monto: Long, fecha: String) =
        Movimiento("x$cat$monto$fecha", Tipo.GASTO, monto, cat, "a", LocalDate.parse(fecha))

    @Test fun heredaDelUltimoMesDefinido() {
        val porMes = mapOf(sep to mapOf("super" to 100L), nov to mapOf("super" to 300L))
        assertEquals(sep to mapOf("super" to 100L), presupuestoVigente(porMes, oct))
        assertEquals(nov, presupuestoVigente(porMes, nov).first)
        assertNull(presupuestoVigente(porMes, YearMonth.of(2026, 8)).first)
    }

    @Test fun avanceYExceso() {
        val movs = listOf(
            gasto("super", 120_000, "2026-10-05"),
            gasto("super", 50_000, "2026-09-05"), // otro mes: no cuenta
            gasto("ocio", 30_000, "2026-10-06"),
        )
        val porMes = mapOf(sep to mapOf("super" to 150_000L, "ocio" to 20_000L))
        val e = calcularPresupuesto(movs, cats, porMes, oct)

        assertEquals(sep, e.origen)
        // Solo gastos; la archivada sin presupuesto ni gasto no aparece.
        assertEquals(listOf("super", "ocio"), e.lineas.map { it.categoria.id })
        val superm = e.lineas[0]
        assertEquals(0.8f, superm.fraccion, 0.001f)
        assertFalse(superm.excedido)
        assertEquals(30_000, superm.restante)
        val ocio = e.lineas[1]
        assertTrue(ocio.excedido)
        assertEquals(-10_000, ocio.restante)
        assertEquals(170_000, e.totalPresupuesto)
        assertEquals(150_000, e.totalGastadoPresupuestado)
        assertEquals(1, e.excedidas)
    }

    @Test fun archivadaConGastoApareceSinPresupuesto() {
        val e = calcularPresupuesto(listOf(gasto("viejo", 5_000, "2026-10-01")), cats, emptyMap(), oct)
        assertNull(e.origen)
        assertEquals(listOf("super", "ocio", "viejo"), e.sinPresupuesto.map { it.categoria.id })
        assertEquals(0, e.totalPresupuesto)
    }
}
