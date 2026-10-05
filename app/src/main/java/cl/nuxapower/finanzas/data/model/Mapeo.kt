package cl.nuxapower.finanzas.data.model

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import java.time.LocalDate

// Mapeo manual a/desde Firestore: sin reflexión, sobrevive a R8 sin reglas extra.

private fun DocumentSnapshot.tipo(): Tipo =
    runCatching { Tipo.valueOf(getString("tipo") ?: "") }.getOrDefault(Tipo.GASTO)

fun DocumentSnapshot.toPersona(): Persona = Persona(
    id = id,
    nombre = getString("nombre").orEmpty(),
    color = getLong("color") ?: 0xFF607D8B,
)

fun Persona.toMap(): Map<String, Any> = mapOf("nombre" to nombre, "color" to color)

fun DocumentSnapshot.toCategoria(): Categoria = Categoria(
    id = id,
    nombre = getString("nombre").orEmpty(),
    icono = getString("icono").orEmpty(),
    color = getLong("color") ?: 0xFF607D8B,
    tipo = tipo(),
    archivada = getBoolean("archivada") ?: false,
    orden = (getLong("orden") ?: 0L).toInt(),
)

fun Categoria.toMap(): Map<String, Any> = mapOf(
    "nombre" to nombre,
    "icono" to icono,
    "color" to color,
    "tipo" to tipo.name,
    "archivada" to archivada,
    "orden" to orden,
)

fun DocumentSnapshot.toMovimiento(): Movimiento? {
    val fecha = runCatching { LocalDate.parse(getString("fecha")) }.getOrNull() ?: return null
    return Movimiento(
        id = id,
        tipo = tipo(),
        monto = getLong("monto") ?: 0L,
        categoriaId = getString("categoriaId").orEmpty(),
        personaId = getString("personaId").orEmpty(),
        fecha = fecha,
        nota = getString("nota").orEmpty(),
        creadoPor = getString("creadoPor").orEmpty(),
        creadoEn = getTimestamp("creadoEn")?.toInstant(),
    )
}

/** Mapa para crear: `creadoEn` lo pone el servidor. */
fun Movimiento.toMapNuevo(): Map<String, Any> = mapOf(
    "tipo" to tipo.name,
    "monto" to monto,
    "categoriaId" to categoriaId,
    "personaId" to personaId,
    "fecha" to fecha.toString(),
    "nota" to nota,
    "creadoPor" to creadoPor,
    "creadoEn" to FieldValue.serverTimestamp(),
)

/** Campos editables de un movimiento existente (no toca creadoPor/creadoEn). */
fun Movimiento.toMapEdicion(): Map<String, Any> = mapOf(
    "tipo" to tipo.name,
    "monto" to monto,
    "categoriaId" to categoriaId,
    "personaId" to personaId,
    "fecha" to fecha.toString(),
    "nota" to nota,
)

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toHogar(): Hogar = Hogar(
    id = id,
    codigoInvitacion = getString("codigoInvitacion").orEmpty(),
    miembros = (get("miembros") as? List<String>).orEmpty(),
    creadoPor = getString("creadoPor").orEmpty(),
)
