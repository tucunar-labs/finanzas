package cl.nuxapower.finanzas

import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.domain.calcularResumen
import cl.nuxapower.finanzas.domain.generarCsv
import cl.nuxapower.finanzas.domain.variacion
import cl.nuxapower.finanzas.util.formatoClp
import cl.nuxapower.finanzas.util.normalizarCodigo
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DominioTest {

    private val cats = listOf(
        Categoria("super", "Supermercado", "🛒", 1, Tipo.GASTO),
        Categoria("ocio", "Ocio", "🎉", 2, Tipo.GASTO, archivada = true),
        Categoria("sueldo", "Sueldo", "💼", 3, Tipo.INGRESO),
    )
    private val pers = listOf(Persona("a", "Alonso", 1), Persona("c", "Cata", 2))

    private fun mov(id: String, tipo: Tipo, monto: Long, cat: String, per: String, fecha: String, nota: String = "") =
        Movimiento(id, tipo, monto, cat, per, LocalDate.parse(fecha), nota, per)

    private val movs = listOf(
        mov("1", Tipo.GASTO, 30_000, "super", "a", "2026-10-02"),
        mov("2", Tipo.GASTO, 10_000, "ocio", "c", "2026-10-03", "cine; \"pop\""),
        mov("3", Tipo.INGRESO, 1_000_000, "sueldo", "a", "2026-10-01"),
        mov("4", Tipo.GASTO, 20_000, "super", "c", "2026-09-15"),
        mov("5", Tipo.INGRESO, 800_000, "sueldo", "c", "2026-09-01"),
    )

    @Test fun formatoClp() {
        assertEquals("$0", formatoClp(0))
        assertEquals("$12.500", formatoClp(12_500))
        assertEquals("$1.234.567", formatoClp(1_234_567))
        assertEquals("-$12.500", formatoClp(-12_500))
    }

    @Test fun codigo() {
        assertEquals("AB12CD", normalizarCodigo(" ab-12 cd99"))
    }

    @Test fun resumenDelMes() {
        // Mes cerrado (hoy = noviembre): compara contra septiembre completo.
        val r = calcularResumen(movs, cats, pers, YearMonth.of(2026, 10), hoy = LocalDate.of(2026, 11, 1))
        assertNull(r.corteDia)
        assertEquals(1_000_000, r.ingresos)
        assertEquals(40_000, r.gastos)
        assertEquals(960_000, r.balance)
        assertEquals(800_000, r.ingresosMesAnterior)
        assertEquals(20_000, r.gastosMesAnterior)
        // La categoría archivada sigue apareciendo en reportes.
        assertEquals(listOf("Supermercado", "Ocio"), r.gastoPorCategoria.map { it.nombre })
        assertEquals(0.75f, r.gastoPorCategoria[0].fraccion, 0.001f)
        assertEquals(20_000, r.gastoPorCategoria[0].montoMesAnterior)
        assertEquals(listOf("Alonso" to 30_000L, "Cata" to 10_000L), r.gastoPorPersona.map { it.nombre to it.monto })
        assertEquals(listOf("Sueldo" to 1_000_000L), r.ingresoPorCategoria.map { it.nombre to it.monto })
        assertEquals(800_000, r.ingresoPorCategoria[0].montoMesAnterior)
        assertEquals(6, r.tendencia.size)
        assertEquals(YearMonth.of(2026, 5), r.tendencia.first().mes)
        assertEquals(listOf(800_000L to 20_000L, 1_000_000L to 40_000L), r.tendencia.takeLast(2).map { it.ingresos to it.gastos })
    }

    @Test fun mesEnCursoComparaMismoPeriodo() {
        // Hoy es 3 de octubre: del mes anterior solo cuentan los días 1..3 (sueldo del 1), no el gasto del 15.
        val r = calcularResumen(movs, cats, pers, YearMonth.of(2026, 10), hoy = LocalDate.of(2026, 10, 3))
        assertEquals(3, r.corteDia)
        assertEquals(800_000, r.ingresosMesAnterior)
        assertEquals(0, r.gastosMesAnterior)
        assertEquals(0, r.gastoPorCategoria.first { it.id == "super" }.montoMesAnterior)
    }

    @Test fun variacionPorcentual() {
        assertEquals(100, variacion(40_000, 20_000))
        assertEquals(-50, variacion(10, 20))
        assertNull(variacion(10, 0))
    }

    @Test fun csv() {
        val csv = generarCsv(movs, cats, pers)
        val lineas = csv.removePrefix("﻿").trimEnd().split("\r\n")
        assertEquals("fecha;tipo;monto;categoria;persona;nota;creado_por", lineas[0])
        assertEquals(6, lineas.size)
        assertEquals("2026-09-01;Ingreso;800000;Sueldo;Cata;;Cata", lineas[1])
        assertTrue(lineas.contains("2026-10-03;Gasto;10000;Ocio;Cata;\"cine; \"\"pop\"\"\";Cata"))
    }
}
