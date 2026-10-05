package cl.nuxapower.finanzas.data.model

import java.time.Instant
import java.time.LocalDate

enum class Tipo { GASTO, INGRESO }

/** Cada miembro del hogar es una persona; su id es el uid de Firebase. */
data class Persona(
    val id: String,
    val nombre: String,
    val color: Long,
)

data class Categoria(
    val id: String,
    val nombre: String,
    val icono: String,
    val color: Long,
    val tipo: Tipo,
    val archivada: Boolean = false,
    val orden: Int = 0,
)

data class Movimiento(
    val id: String,
    val tipo: Tipo,
    /** Monto en CLP, siempre positivo; el signo lo da [tipo]. */
    val monto: Long,
    val categoriaId: String,
    val personaId: String,
    val fecha: LocalDate,
    val nota: String = "",
    val creadoPor: String = "",
    val creadoEn: Instant? = null,
    /** Cambio local aún no confirmado por el servidor (sin conexión). */
    val pendiente: Boolean = false,
)

data class Hogar(
    val id: String,
    val codigoInvitacion: String,
    val miembros: List<String>,
    val creadoPor: String,
)
