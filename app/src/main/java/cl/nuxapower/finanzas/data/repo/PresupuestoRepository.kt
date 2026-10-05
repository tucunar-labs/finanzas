package cl.nuxapower.finanzas.data.repo

import android.util.Log
import cl.nuxapower.finanzas.util.flujo
import com.google.firebase.firestore.FirebaseFirestore
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** households/{hid}/presupuestos/{yyyy-MM} = { montos: { categoriaId: monto } } */
@Singleton
class PresupuestoRepository @Inject constructor(private val db: FirebaseFirestore) {

    fun presupuestos(hid: String): Flow<Map<YearMonth, Map<String, Long>>> =
        db.presupuestos(hid).flujo().map { snap ->
            snap.documents.mapNotNull { d ->
                val mes = runCatching { YearMonth.parse(d.id) }.getOrNull() ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                val montos = (d.get("montos") as? Map<String, Any?>).orEmpty()
                    .mapNotNull { (k, v) -> (v as? Number)?.toLong()?.takeIf { it > 0 }?.let { k to it } }
                    .toMap()
                mes to montos
            }.toMap()
        }

    /** Guarda el presupuesto completo del mes (los montos 0 se omiten). */
    fun guardar(hid: String, mes: YearMonth, montos: Map<String, Long>) {
        db.presupuestos(hid).document(mes.toString())
            .set(mapOf("montos" to montos.filterValues { it > 0 }))
            .addOnFailureListener { Log.w("Presupuestos", "Falló guardar", it) }
    }
}
