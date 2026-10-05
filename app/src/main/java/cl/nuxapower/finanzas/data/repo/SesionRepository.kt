package cl.nuxapower.finanzas.data.repo

import cl.nuxapower.finanzas.di.AppScope
import cl.nuxapower.finanzas.util.flujo
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

sealed interface Sesion {
    data object Cargando : Sesion
    data object SinLogin : Sesion
    data class SinHogar(val uid: String) : Sesion
    data class ConHogar(val uid: String, val hogarId: String) : Sesion
    data class Error(val mensaje: String) : Sesion
}

/** Estado global: quién está logueado y a qué hogar pertenece (users/{uid}.householdId). */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SesionRepository @Inject constructor(
    auth: AuthRepository,
    db: FirebaseFirestore,
    @AppScope scope: CoroutineScope,
) {
    val sesion: StateFlow<Sesion> = auth.usuario
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(Sesion.SinLogin)
            } else {
                db.usuario(user.uid).flujo(metadata = true)
                    .mapNotNull { snap ->
                        val hid = snap.getString("householdId")
                        when {
                            hid != null -> Sesion.ConHogar(user.uid, hid)
                            // Sin dato en caché y sin respuesta del servidor aún: seguimos cargando.
                            snap.metadata.isFromCache -> null
                            else -> Sesion.SinHogar(user.uid)
                        }
                    }
                    .onStart { emit(Sesion.Cargando) }
                    .catch { emit(Sesion.Error(it.message ?: "Error al leer la sesión")) }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, Sesion.Cargando)
}
