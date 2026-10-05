package cl.nuxapower.finanzas.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

val localeCl: Locale = Locale.forLanguageTag("es-CL")

private val diaLargo = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", localeCl)
private val diaCorto = DateTimeFormatter.ofPattern("EEE d MMM", localeCl)
private val mesAnio = DateTimeFormatter.ofPattern("MMMM yyyy", localeCl)

fun String.capitalizar(): String = replaceFirstChar { it.titlecase(localeCl) }

/** "Hoy", "Ayer" o "Sábado 4 de octubre". */
fun formatoDia(fecha: LocalDate, hoy: LocalDate = LocalDate.now()): String = when (fecha) {
    hoy -> "Hoy"
    hoy.minusDays(1) -> "Ayer"
    else -> fecha.format(diaLargo).capitalizar()
}

/** "Hoy", "Ayer" o "sáb 4 oct" (para chips). */
fun formatoDiaCorto(fecha: LocalDate, hoy: LocalDate = LocalDate.now()): String = when (fecha) {
    hoy -> "Hoy"
    hoy.minusDays(1) -> "Ayer"
    else -> fecha.format(diaCorto)
}

fun formatoMes(mes: YearMonth): String = mes.format(mesAnio).capitalizar()

// El DatePicker de Material 3 trabaja en milisegundos UTC a medianoche.
fun LocalDate.aMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun Long.aFechaUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
