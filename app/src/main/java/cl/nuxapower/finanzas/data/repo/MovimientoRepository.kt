package cl.nuxapower.finanzas.data.repo

import android.util.Log
import cl.nuxapower.finanzas.data.model.Movimiento
import cl.nuxapower.finanzas.data.model.toMapEdicion
import cl.nuxapower.finanzas.data.model.toMapNuevo
import cl.nuxapower.finanzas.data.model.toMapRestaurar
import cl.nuxapower.finanzas.data.model.toMovimiento
import cl.nuxapower.finanzas.util.flujo
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Escrituras sin `await`: Firestore las aplica al caché local de inmediato (la UI se actualiza
 * por el listener) y las sincroniza solo cuando hay conexión.
 */
@Singleton
class MovimientoRepository @Inject constructor(private val db: FirebaseFirestore) {

    fun movimientos(hid: String): Flow<List<Movimiento>> =
        db.movimientos(hid).flujo().map { snap -> snap.documents.mapNotNull { it.toMovimiento() } }

    /** Devuelve el id generado localmente (sirve para deshacer). */
    fun crear(hid: String, m: Movimiento): String {
        val ref = db.movimientos(hid).document()
        ref.set(m.toMapNuevo()).log("crear")
        return ref.id
    }

    fun editar(hid: String, m: Movimiento) {
        db.movimientos(hid).document(m.id).update(m.toMapEdicion()).log("editar")
    }

    fun eliminar(hid: String, id: String) {
        db.movimientos(hid).document(id).delete().log("eliminar")
    }

    fun restaurar(hid: String, m: Movimiento) {
        db.movimientos(hid).document(m.id).set(m.toMapRestaurar()).log("restaurar")
    }

    private fun <T> Task<T>.log(op: String) = addOnFailureListener { Log.w("Movimientos", "Falló $op", it) }
}
