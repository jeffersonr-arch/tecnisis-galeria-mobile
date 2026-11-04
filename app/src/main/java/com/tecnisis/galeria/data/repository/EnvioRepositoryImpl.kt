package com.tecnisis.galeria.data.repository

import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.tecnisis.galeria.data.local.GaleriaDatabase
import com.tecnisis.galeria.data.local.ObraEntity
import com.tecnisis.galeria.data.local.SolicitudEntity
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.ObraError
import com.tecnisis.galeria.domain.repository.EnvioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date

/** HU06: Room → Storage (fotos) → Firestore (solicitud y obras en un batch) → Room (enviada). */
class EnvioRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val baseDeDatos: GaleriaDatabase
) : EnvioRepository {

    private val dao = baseDeDatos.obraDao()
    private val metadatosJpg = StorageMetadata.Builder().setContentType("image/jpeg").build()

    override suspend fun subirPendientes(): Result<Unit> = try {
        // Sin sesión no se puede subir; se vuelve a programar al entrar (SesionViewModel)
        val uid = auth.currentUser?.uid
        if (uid != null) {
            dao.solicitudesConEstado(uid, EstadoSolicitud.POR_ENVIAR.valorFirestore)
                .forEach { subir(uid, it.solicitud) }
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo subir la solicitud", e)
        Result.failure(mapear(e))
    }

    private suspend fun subir(uid: String, solicitud: SolicitudEntity) {
        // 1) Fotos: solo las que aún no tienen URL (si se cortó a la mitad, las subidas no se repiten)
        dao.obrasDe(solicitud.id).filter { it.fotoUrl == null }.forEach { obra ->
            val referencia = storage.reference.child(rutaFoto(uid, obra.id))
            referencia.putFile(Uri.fromFile(File(obra.fotoLocal)), metadatosJpg).await()
            dao.marcarFotoSubida(obra.id, referencia.downloadUrl.await().toString())
        }

        // 2) Firestore: la solicitud y sus obras en un solo batch (se guarda todo o nada).
        //    Si ya existe, un intento anterior llegó hasta aquí y solo falta actualizar Room.
        val docSolicitud = db.collection(SOLICITUDES).document(solicitud.id)
        if (!docSolicitud.get(Source.SERVER).await().exists()) {
            val obras = dao.obrasDe(solicitud.id)
            val lote = db.batch()
            lote.set(
                docSolicitud,
                mapOf(
                    "id" to solicitud.id,
                    "uid" to uid,
                    "estado" to EstadoSolicitud.ENVIADA.valorFirestore,
                    "cantidadObras" to obras.size,
                    "obraIds" to obras.map { it.id },
                    "fechaCreacion" to Timestamp(Date(solicitud.creadaEn)),
                    "fechaEnvio" to FieldValue.serverTimestamp()
                )
            )
            obras.forEach { obra -> lote.set(db.collection(OBRAS).document(obra.id), obra.aFirestore(uid)) }
            lote.commit().await()
        }

        // 3) Room: recién ahora figura como enviada en el celular (Mis obras se actualiza sola)
        baseDeDatos.withTransaction {
            dao.cambiarEstadoSolicitud(solicitud.id, EstadoSolicitud.ENVIADA.valorFirestore, System.currentTimeMillis())
            dao.cambiarEstadoObras(solicitud.id, EstadoObra.ENVIADA.valorFirestore)
        }
    }

    private fun ObraEntity.aFirestore(uid: String): Map<String, Any?> = mapOf(
        "id" to id,
        "solicitudId" to solicitudId,
        "uid" to uid,
        "titulo" to titulo,
        "tecnica" to tecnica,
        "altoCm" to altoCm,
        "anchoCm" to anchoCm,
        "profundidadCm" to profundidadCm,
        "fotoUrl" to fotoUrl,
        "fotoRuta" to rutaFoto(uid, id),
        "nota" to nota,
        "estado" to EstadoObra.ENVIADA.valorFirestore,
        "fechaRegistro" to Timestamp(Date(creadaEn)),
        "fechaEnvio" to FieldValue.serverTimestamp()
    )

    private fun mapear(e: Exception): ObraError = when (e) {
        is ObraError -> e
        is FirebaseNetworkException -> ObraError.SinConexion
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> ObraError.SinConexion
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> ObraError.SinPermiso
            else -> ObraError.Desconocido
        }
        is StorageException -> when (e.errorCode) {
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> ObraError.SinConexion
            StorageException.ERROR_NOT_AUTHORIZED,
            StorageException.ERROR_NOT_AUTHENTICATED -> ObraError.SinPermiso
            else -> ObraError.Desconocido
        }
        else -> ObraError.Desconocido
    }

    companion object {
        private const val TAG = "EnvioSolicitud"
        private const val SOLICITUDES = "solicitudes"
        private const val OBRAS = "obras"

        fun rutaFoto(uid: String, obraId: String) = "obras/$uid/$obraId.jpg"
    }
}