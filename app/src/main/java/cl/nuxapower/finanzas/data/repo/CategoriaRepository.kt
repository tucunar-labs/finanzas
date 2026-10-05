package cl.nuxapower.finanzas.data.repo

import android.util.Log
import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.toMap
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

/** Las categorías no se borran: se archivan para no romper el historial. */
@Singleton
class CategoriaRepository @Inject constructor(private val db: FirebaseFirestore) {

    fun guardar(hid: String, c: Categoria) {
        val col = db.categorias(hid)
        val ref = if (c.id.isEmpty()) col.document() else col.document(c.id)
        ref.set(c.toMap()).addOnFailureListener { Log.w("Categorias", "Falló guardar", it) }
    }

    fun archivar(hid: String, id: String, archivada: Boolean) {
        db.categorias(hid).document(id).update("archivada", archivada)
            .addOnFailureListener { Log.w("Categorias", "Falló archivar", it) }
    }

    /** Intercambia la posición de dos categorías. */
    fun intercambiar(hid: String, a: Categoria, b: Categoria) {
        db.batch().apply {
            update(db.categorias(hid).document(a.id), "orden", b.orden)
            update(db.categorias(hid).document(b.id), "orden", a.orden)
        }.commit().addOnFailureListener { Log.w("Categorias", "Falló reordenar", it) }
    }
}
