package cl.nuxapower.finanzas.domain

import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.data.model.Tipo

/**
 * CSV separado por ";" (Excel en español lo abre en columnas directamente) con BOM UTF-8
 * para que los acentos se vean bien. Montos enteros sin separador de miles.
 */
fun generarCsv(movimientos: List<Movimiento>, categorias: List<Categoria>, personas: List<Persona>): String {
    val cat = categorias.associateBy { it.id }
    val per = personas.associateBy { it.id }
    return buildString {
        append('﻿')
        append("fecha;tipo;monto;categoria;persona;nota;creado_por\r\n")
        movimientos.sortedWith(compareBy<Movimiento> { it.fecha }.thenBy { it.creadoEn }).forEach { m ->
            val campos = listOf(
                m.fecha.toString(),
                if (m.tipo == Tipo.GASTO) "Gasto" else "Ingreso",
                m.monto.toString(),
                cat[m.categoriaId]?.nombre.orEmpty(),
                per[m.personaId]?.nombre.orEmpty(),
                m.nota,
                per[m.creadoPor]?.nombre.orEmpty(),
            )
            append(campos.joinToString(";") { escapar(it) })
            append("\r\n")
        }
    }
}

private fun escapar(v: String): String =
    if (v.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
