package cl.nuxapower.finanzas.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.nuxapower.finanzas.data.model.Categoria
import cl.nuxapower.finanzas.data.model.Hogar
import cl.nuxapower.finanzas.data.model.Persona
import cl.nuxapower.finanzas.data.repo.AuthRepository
import cl.nuxapower.finanzas.data.repo.HogarRepository
import cl.nuxapower.finanzas.data.repo.Sesion
import cl.nuxapower.finanzas.data.repo.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class HomeEstado(
    val hogar: Hogar? = null,
    val personas: List<Persona> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val uid: String = "",
)

/** Etapa 1: muestra el hogar, sus miembros y categorías para verificar el modelo. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    sesion: SesionRepository,
    hogares: HogarRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    val estado: StateFlow<HomeEstado> = sesion.sesion
        .filterIsInstance<Sesion.ConHogar>()
        .flatMapLatest { s ->
            combine(hogares.hogar(s.hogarId), hogares.personas(s.hogarId), hogares.categorias(s.hogarId)) { h, p, c ->
                HomeEstado(h, p, c, s.uid)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeEstado())

    fun salir() = auth.salir()
}
