package cl.nuxapower.finanzas.ui.hogar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.repo.AuthRepository
import cl.nuxapower.finanzas.data.repo.HogarRepository
import cl.nuxapower.finanzas.util.normalizarCodigo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupEstado(
    val codigo: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HogarSetupViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val hogares: HogarRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(SetupEstado())
    val estado: StateFlow<SetupEstado> = _estado.asStateFlow()

    fun onCodigo(texto: String) = _estado.update { it.copy(codigo = normalizarCodigo(texto), error = null) }

    /** Al terminar, SesionRepository detecta users/{uid}.householdId y la app cambia de pantalla sola. */
    fun crear() = ejecutar { hogares.crearHogar(it) }

    fun unirse() {
        val codigo = _estado.value.codigo
        if (codigo.length != 6) {
            _estado.update { it.copy(error = "El código tiene 6 caracteres") }
            return
        }
        ejecutar { hogares.unirse(it, codigo) }
    }

    fun salir() = auth.salir()

    private fun ejecutar(accion: suspend (com.google.firebase.auth.FirebaseUser) -> Unit) {
        val user = auth.actual ?: return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            runCatching { accion(user) }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = e.message ?: "Error") } }
        }
    }
}
