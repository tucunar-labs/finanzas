package cl.nuxapower.finanzas.di

import cl.nuxapower.finanzas.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun auth(): FirebaseAuth = FirebaseAuth.getInstance().apply {
        if (BuildConfig.EMULADOR_HOST.isNotEmpty()) useEmulator(BuildConfig.EMULADOR_HOST, 9099)
    }

    @Provides
    @Singleton
    fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Debug: emulador local de Firestore (scripts/dev.sh). Debe ir antes de cualquier uso.
        if (BuildConfig.EMULADOR_HOST.isNotEmpty()) useEmulator(BuildConfig.EMULADOR_HOST, 8085)
        // Caché persistente explícita: los movimientos se guardan local y sincronizan solos.
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100L * 1024 * 1024)
                    .build()
            )
            .build()
    }

    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
