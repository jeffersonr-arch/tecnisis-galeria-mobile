package com.tecnisis.galeria.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.tecnisis.galeria.data.local.GaleriaDatabase
import com.tecnisis.galeria.data.local.ObraEntity
import com.tecnisis.galeria.data.local.SolicitudEntity
import com.tecnisis.galeria.data.work.SincronizacionSolicitudWorker
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.ObraError
import com.tecnisis.galeria.domain.repository.SeguimientoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * HU07: Firestore → Room para el socio. Mis obras siguen leyendo solo de Room (funciona sin internet);
 * esto le trae la decisión del Anfitrión y, si cambió de celular, la solicitud que le falta (sin foto).
 */
class SeguimientoRepositoryImpl(
    private val contexto: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val baseDeDatos: GaleriaDatabase
) : SeguimientoRepository {

    private val dao = baseDeDatos.obraDao()

    override suspend fun sincronizar(): Result<Unit> = try {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            // whereEqualTo("uid", uid): las reglas solo dejan leer las solicitudes propias
            db.collection(SOLICITUDES).whereEqualTo("uid", uid).get(Source.SERVER).await()
                .documents.forEach { actualizar(uid, it) }
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo sincronizar las solicitudes", e)
        Result.failure(mapear(e))
    }

    private suspend fun actualizar(uid: String, doc: DocumentSnapshot) {
        val local = dao.solicitud(doc.id)
        // El borrador y el pendiente de envío son de la HU06: aquí no se tocan
        if (local != null) {
            val estadoLocal = EstadoSolicitud.desdeValor(local.estado)
            if (estadoLocal == EstadoSolicitud.BORRADOR || estadoLocal == EstadoSolicitud.POR_ENVIAR) return
        }
        val obras = db.collection(OBRAS)
            .whereEqualTo("solicitudId", doc.id)
            .whereEqualTo("uid", uid)
            .get(Source.SERVER).await().documents

        baseDeDatos.withTransaction {
            if (local == null) {
                // No está en el celular (cambió de celular o borró los datos): se agrega sin foto
                dao.guardarSolicitud(doc.aSolicitud(uid))
                obras.forEach { dao.guardarObra(it.aObra(uid)) }
            } else {
                dao.guardarResolucion(
                    doc.id,
                    doc.getString("estado") ?: local.estado,
                    doc.getString("motivoRechazo"),
                    doc.getTimestamp("fechaResolucion")?.toDate()?.time
                )
                obras.forEach { obra ->
                    obra.getString("estado")?.let { estado ->
                        dao.guardarEstadoYComentario(obra.id, estado, obra.comentarioVeredicto())
                    }
                }
            }
        }
    }

    override fun programarSincronizacion() {
        val solicitud = OneTimeWorkRequestBuilder<SincronizacionSolicitudWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // REPLACE: si llegan dos avisos seguidos, basta con una sincronización
        WorkManager.getInstance(contexto).enqueueUniqueWork(TRABAJO, ExistingWorkPolicy.REPLACE, solicitud)
    }

    private fun DocumentSnapshot.aSolicitud(uid: String): SolicitudEntity {
        val enviada = getTimestamp("fechaEnvio")?.toDate()?.time
        return SolicitudEntity(
            id = id,
            uid = uid,
            estado = getString("estado") ?: EstadoSolicitud.ENVIADA.valorFirestore,
            creadaEn = getTimestamp("fechaCreacion")?.toDate()?.time ?: enviada ?: 0L,
            enviadaEn = enviada,
            motivoRechazo = getString("motivoRechazo"),
            resueltaEn = getTimestamp("fechaResolucion")?.toDate()?.time
        )
    }

    /** fotoLocal vacía: Mis obras dibuja las manchas de pigmento en lugar de la foto. */
    private fun DocumentSnapshot.aObra(uid: String) = ObraEntity(
        id = id,
        solicitudId = getString("solicitudId").orEmpty(),
        uid = uid,
        titulo = getString("titulo").orEmpty(),
        tecnica = getString("tecnica").orEmpty(),
        altoCm = getDouble("altoCm") ?: 0.0,
        anchoCm = getDouble("anchoCm") ?: 0.0,
        profundidadCm = getDouble("profundidadCm"),
        fotoLocal = "",
        fotoUrl = getString("fotoUrl"),
        nota = getString("nota").orEmpty(),
        estado = getString("estado") ?: EstadoObra.ENVIADA.valorFirestore,
        creadaEn = getTimestamp("fechaRegistro")?.toDate()?.time ?: 0L,
        comentarioVeredicto = comentarioVeredicto()
    )

    /** HU08: lo escribe registrarVeredicto; vacío o ausente = sin comentario. */
    private fun DocumentSnapshot.comentarioVeredicto(): String? =
        getString("veredictoComentario")?.takeIf { it.isNotBlank() }

    private fun mapear(e: Exception): ObraError = when (e) {
        is FirebaseNetworkException -> ObraError.SinConexion
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> ObraError.SinConexion
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> ObraError.SinPermiso
            else -> ObraError.Desconocido
        }
        else -> ObraError.Desconocido
    }

    private companion object {
        const val TAG = "SeguimientoSolicitud"
        const val SOLICITUDES = "solicitudes"
        const val OBRAS = "obras"
        const val TRABAJO = "sincronizar_solicitudes"
    }
}