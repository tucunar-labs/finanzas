package cl.nuxapower.finanzas.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CategoriaWidget(val id: String, val icono: String, val nombre: String)

/** Lo que muestra el widget. [restante] es null si no hay presupuesto definido para el mes. */
data class DatosWidget(
    val categorias: List<CategoriaWidget>,
    val mes: String,
    val gastado: Long,
    val restante: Long?,
)

/**
 * Copia local de lo que muestra el widget: así se dibuja al instante y sin red, sin abrir
 * Firestore desde el proceso del widget. La actualiza [WidgetSync] mientras la app corre.
 */
object WidgetCache {
    private const val PREFS = "widget"
    private const val CLAVE = "datos"

    /** null = sin sesión con hogar (el widget invita a abrir la app). */
    fun guardar(context: Context, datos: DatosWidget?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (datos == null) {
            prefs.remove(CLAVE)
        } else {
            val cats = JSONArray()
            datos.categorias.forEach { cats.put(JSONObject().put("id", it.id).put("icono", it.icono).put("nombre", it.nombre)) }
            val json = JSONObject()
                .put("categorias", cats)
                .put("mes", datos.mes)
                .put("gastado", datos.gastado)
            datos.restante?.let { json.put("restante", it) }
            prefs.putString(CLAVE, json.toString())
        }
        prefs.apply()
    }

    fun leer(context: Context): DatosWidget? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val arr = json.getJSONArray("categorias")
            DatosWidget(
                categorias = (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    CategoriaWidget(o.getString("id"), o.getString("icono"), o.getString("nombre"))
                },
                mes = json.optString("mes"),
                gastado = json.optLong("gastado"),
                restante = if (json.has("restante")) json.getLong("restante") else null,
            )
        }.getOrNull()
    }
}
