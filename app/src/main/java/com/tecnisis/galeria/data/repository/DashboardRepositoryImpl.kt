package com.tecnisis.galeria.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.tecnisis.galeria.domain.model.DashboardError
import com.tecnisis.galeria.domain.model.ExpertoDashboard
import com.tecnisis.galeria.domain.model.ObraEvaluada
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.repository.DashboardRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * HU11: obras con veredicto y Expertos en tiempo real para el Gerente.
 * Un solo filtro whereIn sobre estado: sin índice compuesto y es lo que exige la regla de Firestore.
 */
class DashboardRepositoryImpl(private val db: FirebaseFirestore) : DashboardRepository {

    override fun observarObrasEvaluadas(): Flow<List<ObraEvaluada>> = callbackFlow {
        val registro = db.collection(OBRAS).whereIn("estado", ESTADOS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapear(error))
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map { it.aObraEvaluada() })
            }
        awaitClose { registro.remove() }
    }

    override fun observarExpertos(): Flow<List<ExpertoDashboard>> = callbackFlow {
        val registro = db.collection(EXPERTOS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapear(error))
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map { it.aExperto() })
            }
        awaitClose { registro.remove() }
    }

    private fun DocumentSnapshot.aObraEvaluada() = ObraEvaluada(
        id = id,
        aceptada = getString("estado") != "rechazada",
        tecnica = Tecnica.desdeValor(getString("tecnica")),
        nombreSocio = getString("nombreSocio"),
        numeroSocio = getString("numeroSocio"),
        evaluadaPor = getString("evaluadaPor"),
        fechaVeredicto = getTimestamp("fechaVeredicto")?.toDate()?.time
    )

    private fun DocumentSnapshot.aExperto() = ExpertoDashboard(
        uid = id,
        nombre = getString("nombre")?.takeIf { it.isNotBlank() } ?: "Especialista de Arte",
        activo = getString("estado") == "activo",
        carga = (getLong("carga") ?: 0L).toInt().coerceAtLeast(0)
    )

    private fun mapear(e: FirebaseFirestoreException): DashboardError = when (e.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> DashboardError.SinConexion
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> DashboardError.SinPermiso
        else -> DashboardError.Desconocido
    }

    private companion object {
        const val OBRAS = "obras"
        const val EXPERTOS = "expertos"
        val ESTADOS = listOf("aceptada", "lista_exhibicion", "rechazada")
    }
}