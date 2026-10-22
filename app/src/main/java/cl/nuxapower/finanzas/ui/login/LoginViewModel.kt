package cl.nuxapower.finanzas.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginEstado(val cargando: Boolean = false, val error: String? = null)

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LoginEstado())
    val estado: StateFlow<LoginEstado> = _estado.asStateFlow()

    fun iniciando() {
        _estado.value = LoginEstado(cargando = true)
    }

    fun entrar(idToken: String) = viewModelScope.launch {
        runCatching { auth.entrarConGoogle(idToken) }
            .onSuccess { _estado.value = LoginEstado() }
            .onFailure { _estado.value = LoginEstado(error = "No se pudo iniciar sesión: ${it.message}") }
    }

    fun entrarDev() = viewModelScope.launch {
        _estado.value = LoginEstado(cargando = true)
        runCatching { auth.entrarDev("dev@example.com", "Dev") }
            .onSuccess { _estado.value = LoginEstado() }
            .onFailure { _estado.value = LoginEstado(error = "Login dev falló (¿corre scripts/dev.sh?): ${it.message}") }
    }

    fun falloCredencial(e: Throwable) {
        _estado.value = LoginEstado(error = mensajeCredencial(e))
    }
}
