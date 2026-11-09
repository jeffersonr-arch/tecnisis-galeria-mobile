package com.tecnisis.galeria.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.tecnisis.galeria.domain.model.HistoricoError
import com.tecnisis.galeria.domain.model.ObraRechazada
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.repository.HistoricoRechazosRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * HU09: obras rechazadas en tiempo real para el Gerente.
 * Un solo filtro (estado == "rechazada"): no necesita índice compuesto y es lo que exige la regla de Firestore.
 * El orden lo pone el caso de uso.
 */
class HistoricoRechazosRepositoryImpl(private val db: FirebaseFirestore) : HistoricoRechazosRepository {

    override fun observarRechazadas(): Flow<List<ObraRechazada>> = callbackFlow {
        val registro = db.collection(OBRAS).whereEqualTo("estado", "rechazada")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapear(error))
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map { it.aObraRechazada() })
            }
        awaitClose { registro.remove() }
    }

    private fun DocumentSnapshot.aObraRechazada() = ObraRechazada(
        id = id,
        titulo = getString("titulo").orEmpty(),
        tecnica = Tecnica.desdeValor(getString("tecnica")) ?: Tecnica.TECNICA_MIXTA,
        altoCm = getDouble("altoCm") ?: 0.0,
        anchoCm = getDouble("anchoCm") ?: 0.0,
        profundidadCm = getDouble("profundidadCm"),
        nombreSocio = getString("nombreSocio"),
        numeroSocio = getString("numeroSocio"),
        evaluadaPorNombre = getString("evaluadaPorNombre"),
        motivo = getString("veredictoComentario").orEmpty(),
        fechaVeredicto = getTimestamp("fechaVeredicto")?.toDate()?.time,
        documentoRechazo = (get("documentoRechazo") as? List<*>).orEmpty().filterIsInstance<String>()
    )

    private fun mapear(e: FirebaseFirestoreException): HistoricoError = when (e.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> HistoricoError.SinConexion
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> HistoricoError.SinPermiso
        else -> HistoricoError.Desconocido
    }

    private companion object {
        const val OBRAS = "obras"
    }
}