package cl.nuxapower.finanzas.data.repo

import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Hogar
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.data.model.categoriasPorDefecto
import cl.nuxapower.finanzas.data.model.coloresPersona
import cl.nuxapower.finanzas.data.model.toCategoria
import cl.nuxapower.finanzas.data.model.toHogar
import cl.nuxapower.finanzas.data.model.toMap
import cl.nuxapower.finanzas.data.model.toPersona
import cl.nuxapower.finanzas.util.flujo
import cl.nuxapower.finanzas.util.generarCodigoInvitacion
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class HogarException(mensaje: String) : Exception(mensaje)

@Singleton
class HogarRepository @Inject constructor(private val db: FirebaseFirestore) {

    fun hogar(hid: String): Flow<Hogar?> =
        db.hogar(hid).flujo().map { if (it.exists()) it.toHogar() else null }

    fun personas(hid: String): Flow<List<Persona>> =
        db.personas(hid).flujo().map { snap -> snap.documents.map { it.toPersona() }.sortedBy { it.nombre } }

    /** Todas las categorías (incluidas archivadas), ordenadas por `orden`. */
    fun categorias(hid: String): Flow<List<Categoria>> =
        db.categorias(hid).orderBy("orden").flujo().map { snap -> snap.documents.map { it.toCategoria() } }

    /**
     * Crea el hogar con su código de invitación, la persona del creador y las categorías por defecto.
     * Requiere conexión (hay que verificar que el código no exista).
     */
    suspend fun crearHogar(user: FirebaseUser): String = conTimeout {
        val codigo = codigoLibre()
        val hogarRef = db.hogares().document()
        val hid = hogarRef.id

        // Lote 1: hogar + invitación. Las reglas validan la invitación contra el hogar (getAfter).
        db.batch().apply {
            set(
                hogarRef,
                mapOf(
                    "codigoInvitacion" to codigo,
                    "miembros" to listOf(user.uid),
                    "creadoPor" to user.uid,
                    "creadoEn" to FieldValue.serverTimestamp(),
                )
            )
            set(db.invitacion(codigo), mapOf("householdId" to hid))
        }.commit().await()

        // Lote 2: persona, categorías y, al final, el puntero del usuario (activa la sesión).
        db.batch().apply {
            set(db.personas(hid).document(user.uid), personaDe(user, coloresPersona[0]).toMap())
            categoriasPorDefecto.forEach { set(db.categorias(hid).document(), it.toMap()) }
            set(db.usuario(user.uid), mapOf("householdId" to hid))
        }.commit().await()
        hid
    }

    /** Une al usuario a un hogar existente usando el código. Máximo 2 miembros (lo exigen las reglas). */
    suspend fun unirse(user: FirebaseUser, codigoIngresado: String): String = conTimeout {
        val inv = db.invitacion(codigoIngresado).get(Source.SERVER).await()
        val hid = inv.getString("householdId") ?: throw HogarException("Código no válido")
        try {
            db.batch().apply {
                update(db.hogar(hid), "miembros", FieldValue.arrayUnion(user.uid))
                set(db.personas(hid).document(user.uid), personaDe(user, coloresPersona[1]).toMap())
                set(db.usuario(user.uid), mapOf("householdId" to hid))
            }.commit().await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw HogarException("Ese hogar ya tiene 2 miembros")
            }
            throw e
        }
        hid
    }

    fun actualizarPersona(hid: String, persona: Persona) {
        // Sin await: con offline se aplica local al tiro y sincroniza después.
        db.personas(hid).document(persona.id).set(persona.toMap())
    }

    private suspend fun codigoLibre(): String {
        repeat(5) {
            val c = generarCodigoInvitacion()
            if (!db.invitacion(c).get(Source.SERVER).await().exists()) return c
        }
        throw HogarException("No se pudo generar un código, intenta de nuevo")
    }

    private fun personaDe(user: FirebaseUser, color: Long) = Persona(
        id = user.uid,
        nombre = user.displayName?.substringBefore(' ')?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')
            ?: "Yo",
        color = color,
    )

    private suspend fun <T> conTimeout(bloque: suspend () -> T): T = try {
        withTimeout(20_000) { bloque() }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        throw HogarException("Sin conexión: para crear o unirte a un hogar necesitas internet")
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
            throw HogarException("Sin conexión: para crear o unirte a un hogar necesitas internet")
        }
        throw e
    }
}
