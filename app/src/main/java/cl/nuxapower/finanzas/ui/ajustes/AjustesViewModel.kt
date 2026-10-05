package cl.nuxapower.finanzas.ui.ajustes

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.model.Hogar
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.data.repo.AuthRepository
import cl.nuxapower.finanzas.data.repo.HogarDatos
import cl.nuxapower.finanzas.data.repo.HogarRepository
import cl.nuxapower.finanzas.domain.generarCsv
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class AjustesEstado(
    val hogar: Hogar? = null,
    val personas: List<Persona> = emptyList(),
    val uid: String = "",
    val totalMovimientos: Int = 0,
) {
    val yo: Persona? get() = personas.find { it.id == uid }
}

@HiltViewModel
class AjustesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val datos: HogarDatos,
    private val hogares: HogarRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    val estado: StateFlow<AjustesEstado> =
        combine(datos.hogar, datos.personas, datos.movimientos) { h, p, m ->
            AjustesEstado(h, p, datos.uid.orEmpty(), m?.size ?: 0)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AjustesEstado())

    fun guardarPerfil(nombre: String, color: Long) {
        val hid = datos.hogarId ?: return
        val uid = datos.uid ?: return
        hogares.actualizarPersona(hid, Persona(uid, nombre.trim().take(40), color))
    }

    /**
     * Escribe el CSV en caché y devuelve un Uri compartible (FileProvider).
     * Con [desde]/[hasta] null exporta todo.
     */
    suspend fun exportar(desde: LocalDate?, hasta: LocalDate?): Uri? = withContext(Dispatchers.IO) {
        val movs = datos.movimientos.value ?: return@withContext null
        val filtrados = movs.filter { m ->
            (desde == null || !m.fecha.isBefore(desde)) && (hasta == null || !m.fecha.isAfter(hasta))
        }
        val dir = File(context.cacheDir, "exportes").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val sufijo = if (desde != null && hasta != null) "${desde}_a_$hasta" else "todo_${LocalDate.now()}"
        val archivo = File(dir, "movimientos_$sufijo.csv")
        archivo.writeText(generarCsv(filtrados, datos.categorias.value, datos.personas.value), Charsets.UTF_8)
        FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
    }

    fun salir() = auth.salir()
}
