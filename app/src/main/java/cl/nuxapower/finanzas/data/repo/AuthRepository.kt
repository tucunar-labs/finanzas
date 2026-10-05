package cl.nuxapower.finanzas.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class AuthRepository @Inject constructor(private val auth: FirebaseAuth) {

    val usuario: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val actual: FirebaseUser? get() = auth.currentUser

    suspend fun entrarConGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    fun salir() = auth.signOut()
}
