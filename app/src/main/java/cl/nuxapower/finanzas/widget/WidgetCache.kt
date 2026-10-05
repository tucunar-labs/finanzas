package cl.nuxapower.finanzas.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CategoriaWidget(val id: String, val icono: String, val nombre: String)

/**
 * Copia local de las categorías de gasto para el widget: así se dibuja al instante y sin red,
 * sin abrir Firestore desde el proceso del widget. La actualiza [WidgetSync] mientras la app corre.
 */
object WidgetCache {
    private const val PREFS = "widget"
    private const val CLAVE = "categorias"

    /** null = sin sesión con hogar (el widget invita a abrir la app). */
    fun guardar(context: Context, categorias: List<CategoriaWidget>?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (categorias == null) {
            prefs.remove(CLAVE)
        } else {
            val arr = JSONArray()
            categorias.forEach { arr.put(JSONObject().put("id", it.id).put("icono", it.icono).put("nombre", it.nombre)) }
            prefs.putString(CLAVE, arr.toString())
        }
        prefs.apply()
    }

    fun leer(context: Context): List<CategoriaWidget>? {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE, null) ?: return null
        val arr = JSONArray(json)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            CategoriaWidget(o.getString("id"), o.getString("icono"), o.getString("nombre"))
        }
    }
}
