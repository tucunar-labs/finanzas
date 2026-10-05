package cl.nuxapower.finanzas.data.repo

import com.google.firebase.firestore.FirebaseFirestore

/** Rutas de Firestore en un solo lugar. Todo lo del hogar vive bajo households/{hogarId}. */
internal fun FirebaseFirestore.usuario(uid: String) = collection("users").document(uid)
internal fun FirebaseFirestore.invitacion(codigo: String) = collection("invites").document(codigo)
internal fun FirebaseFirestore.hogares() = collection("households")
internal fun FirebaseFirestore.hogar(hid: String) = hogares().document(hid)
internal fun FirebaseFirestore.personas(hid: String) = hogar(hid).collection("personas")
internal fun FirebaseFirestore.categorias(hid: String) = hogar(hid).collection("categorias")
internal fun FirebaseFirestore.movimientos(hid: String) = hogar(hid).collection("movimientos")
internal fun FirebaseFirestore.presupuestos(hid: String) = hogar(hid).collection("presupuestos")
