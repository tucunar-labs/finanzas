package cl.nuxapower.finanzas.util

import java.security.SecureRandom

// Sin 0/O, 1/I/L para que se pueda dictar sin confusiones.
private const val ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
private val random = SecureRandom()

fun generarCodigoInvitacion(): String = buildString { repeat(6) { append(ALFABETO[random.nextInt(ALFABETO.length)]) } }

/** Normaliza lo que escribe el usuario: mayúsculas, sin espacios. */
fun normalizarCodigo(texto: String): String = texto.uppercase().filter { it.isLetterOrDigit() }.take(6)
