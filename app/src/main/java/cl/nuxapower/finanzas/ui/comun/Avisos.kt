package cl.nuxapower.finanzas.ui.comun

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Snackbars con "Deshacer" a nivel de app: viven en el scope raíz, así que siguen visibles
 * aunque la pantalla que los lanzó ya se haya cerrado (ej. eliminar desde la edición).
 */
class Avisos(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun mostrar(mensaje: String, accion: String? = null, onAccion: () -> Unit = {}) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val r = host.showSnackbar(
                message = mensaje,
                actionLabel = accion,
                withDismissAction = accion == null,
                duration = if (accion != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (r == SnackbarResult.ActionPerformed) onAccion()
        }
    }

    fun deshacer(mensaje: String, onDeshacer: () -> Unit) = mostrar(mensaje, "Deshacer", onDeshacer)
}

val LocalAvisos = staticCompositionLocalOf<Avisos> { error("Avisos no provisto") }
