package com.tecnisis.galeria.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.tecnisis.galeria.domain.model.ObraProyectada
import com.tecnisis.galeria.domain.model.ProyeccionError
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.repository.ProyeccionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * HU12: documentos_venta completa en tiempo real para el Gerente (sin filtros ni índice).
 * Los escribe solo la Cloud Function registrarTasacion; los montos vienen en céntimos.
 */
class ProyeccionRepositoryImpl(private val db: FirebaseFirestore) : ProyeccionRepository {

    override fun observarObrasTasadas(): Flow<List<ObraProyectada>> = callbackFlow {
        val registro = db.collection(DOCUMENTOS_VENTA)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapear(error))
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map { it.aObraProyectada() })
            }
        awaitClose { registro.remove() }
    }

    private fun DocumentSnapshot.aObraProyectada() = ObraProyectada(
        obraId = id,
        uid = getString("uid").orEmpty(),
        titulo = getString("titulo").orEmpty(),
        tecnica = Tecnica.desdeValor(getString("tecnica")),
        nombreSocio = getString("nombreSocio"),
        numeroSocio = getString("numeroSocio"),
        pvpCentimos = getLong("pvpCentimos") ?: 0L,
        porcentaje = getLong("porcentaje")?.toInt() ?: 0,
        montoGaleriaCentimos = getLong("montoGaleriaCentimos") ?: 0L,
        montoSocioCentimos = getLong("montoSocioCentimos") ?: 0L,
        fechaTasacion = getTimestamp("fechaTasacion")?.toDate()?.time
    )

    private fun mapear(e: FirebaseFirestoreException): ProyeccionError = when (e.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> ProyeccionError.SinConexion
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> ProyeccionError.SinPermiso
        else -> ProyeccionError.Desconocido
    }

    private companion object {
        const val DOCUMENTOS_VENTA = "documentos_venta"
    }
}