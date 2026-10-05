package cl.nuxapower.finanzas.util

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Listener en tiempo real de un documento como Flow. Lee de caché primero si está offline. */
fun DocumentReference.flujo(metadata: Boolean = false): Flow<DocumentSnapshot> = callbackFlow {
    val cambios = if (metadata) MetadataChanges.INCLUDE else MetadataChanges.EXCLUDE
    val reg = addSnapshotListener(cambios) { snap, err ->
        if (err != null) close(err) else if (snap != null) trySend(snap)
    }
    awaitClose { reg.remove() }
}

/** Listener en tiempo real de una consulta como Flow. */
fun Query.flujo(): Flow<QuerySnapshot> = callbackFlow {
    val reg = addSnapshotListener { snap, err ->
        if (err != null) close(err) else if (snap != null) trySend(snap)
    }
    awaitClose { reg.remove() }
}
