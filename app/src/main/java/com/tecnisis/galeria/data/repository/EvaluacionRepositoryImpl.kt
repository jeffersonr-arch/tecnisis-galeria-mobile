package com.tecnisis.galeria.data.repository

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.tecnisis.galeria.domain.model.EstadoEspecialista
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.EvaluacionError
import com.tecnisis.galeria.domain.model.ObraAsignada
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.TipoVeredicto
import com.tecnisis.galeria.domain.repository.EvaluacionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException
import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.tecnisis.galeria.data.pdf.GeneradorPdf
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.documentoRechazoDe
import com.tecnisis.galeria.domain.model.rutaDocumentoObra
import java.io.File

/**
 * HU08: Firestore en tiempo real para el Especialista de Arte y la Cloud Function registrarVeredicto.
 * Las reglas solo le dejan leer las obras con expertoUid == su uid, por eso cada consulta lleva ese filtro.
 * HU09: al rechazar, el celular genera el documento por duplicado y lo sube ANTES de llamar a la función,
 * que revisa que exista: si falla la subida, no se guarda nada.
 */
class EvaluacionRepositoryImpl(
    private val contexto: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
    private val generador: GeneradorPdf
) : EvaluacionRepository {

    private val metadatosPdf = StorageMetadata.Builder().setContentType("application/pdf").build()

    override fun observarAsignadas(): Flow<List<ObraAsignada>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            close(EvaluacionError.SinPermiso)
            return@callbackFlow
        }
        // Un solo filtro (no necesita índice compuesto). La app separa "Por evaluar" y "Evaluadas" por el estado
        val registro = db.collection(OBRAS).whereEqualTo("expertoUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapear(error))
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map { it.aObraAsignada() })
            }
        awaitClose { registro.remove() }
    }

    override fun observarObra(id: String): Flow<ObraAsignada?> = callbackFlow {
        val registro = db.collection(OBRAS).document(id).addSnapshotListener { doc, error ->
            if (error != null) {
                close(mapear(error))
                return@addSnapshotListener
            }
            trySend(if (doc != null && doc.exists()) doc.aObraAsignada() else null)
        }
        awaitClose { registro.remove() }
    }

    override fun observarMiFicha(): Flow<PerfilEspecialista?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            close(EvaluacionError.SinPermiso)
            return@callbackFlow
        }
        val registro = db.collection(EXPERTOS).document(uid).addSnapshotListener { doc, error ->
            if (error != null) {
                close(mapear(error))
                return@addSnapshotListener
            }
            trySend(if (doc != null && doc.exists()) doc.aFicha() else null)
        }
        awaitClose { registro.remove() }
    }

    override suspend fun registrarVeredicto(obraId: String, tipo: TipoVeredicto, comentario: String): Result<Unit> =
        try {
            if (tipo == TipoVeredicto.RECHAZAR) subirDocumentoRechazo(obraId, comentario)
            functions.getHttpsCallable("registrarVeredicto")
                .call(mapOf("obraId" to obraId, "veredicto" to tipo.valorFuncion, "comentario" to comentario))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo registrar el veredicto", e)
            Result.failure(mapear(e))
        }

    /** HU09: dibuja las 2 copias en cache/documentos y las sube a documentos_obras/{obraId}/. */
    private suspend fun subirDocumentoRechazo(obraId: String, motivo: String) {
        val uid = auth.currentUser?.uid ?: throw EvaluacionError.SinPermiso
        // Lo que va en el PDF, leído del servidor (sin internet falla aquí, antes de subir nada)
        val doc = db.collection(OBRAS).document(obraId).get(Source.SERVER).await()
        if (!doc.exists()) throw EvaluacionError.NoEncontrada
        val obra = doc.aObraAsignada()
        if (!obra.porEvaluar) throw EvaluacionError.YaEvaluada
        val nombre = db.collection(EXPERTOS).document(uid).get(Source.SERVER).await().getString("nombre").orEmpty()
        val documento = documentoRechazoDe(obra, motivo, nombre, System.currentTimeMillis())

        CopiaDocumento.entries.forEach { copia ->
            val archivo = File(File(contexto.cacheDir, "documentos"), "obra-$obraId-${copia.archivo}")
            try {
                generador.generarRechazoObra(documento, copia, archivo)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo generar ${copia.archivo}", e)
                throw EvaluacionError.PdfNoGenerado
            }
            storage.reference.child(rutaDocumentoObra(obraId, copia))
                .putFile(Uri.fromFile(archivo), metadatosPdf).await()
        }
    }

    private fun DocumentSnapshot.aObraAsignada() = ObraAsignada(
        id = id,
        solicitudId = getString("solicitudId").orEmpty(),
        titulo = getString("titulo").orEmpty(),
        tecnica = Tecnica.desdeValor(getString("tecnica")) ?: Tecnica.TECNICA_MIXTA,
        altoCm = getDouble("altoCm") ?: 0.0,
        anchoCm = getDouble("anchoCm") ?: 0.0,
        profundidadCm = getDouble("profundidadCm"),
        nota = getString("nota").orEmpty(),
        fotoRuta = getString("fotoRuta").orEmpty(),
        descripcionIa = getString("descripcionIa"),
        nombreSocio = getString("nombreSocio"),   // HU08: los copia decidirSolicitud al aprobar
        numeroSocio = getString("numeroSocio"),
        estado = EstadoObra.desdeValor(getString("estado")),
        fechaAsignacion = getTimestamp("fechaAsignacion")?.toDate()?.time,
        fechaEnvio = getTimestamp("fechaEnvio")?.toDate()?.time,
        comentarioVeredicto = getString("veredictoComentario")?.takeIf { it.isNotBlank() },
        fechaVeredicto = getTimestamp("fechaVeredicto")?.toDate()?.time,
        documentoRechazo = (get("documentoRechazo") as? List<*>).orEmpty().filterIsInstance<String>() // HU09
    )

    private fun DocumentSnapshot.aFicha() = PerfilEspecialista(
        uid = id,
        nombre = getString("nombre").orEmpty(),
        tecnicas = (get("tecnicas") as? List<*>).orEmpty()
            .mapNotNull { Tecnica.desdeValor(it as? String) }
            .toSet(),
        estado = EstadoEspecialista.desdeValor(getString("estado")),
        carga = getLong("carga")?.toInt() ?: 0
    )

    private fun mapear(e: Exception): EvaluacionError = when (e) {
        is EvaluacionError -> e
        is FirebaseFunctionsException -> when {
            e.cause is IOException -> EvaluacionError.SinConexion
            e.code == FirebaseFunctionsException.Code.UNAVAILABLE ||
                    e.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> EvaluacionError.SinConexion
            e.code == FirebaseFunctionsException.Code.PERMISSION_DENIED ||
                    e.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> EvaluacionError.SinPermiso
            e.code == FirebaseFunctionsException.Code.NOT_FOUND -> EvaluacionError.NoEncontrada
            e.code == FirebaseFunctionsException.Code.INVALID_ARGUMENT -> EvaluacionError.ComentarioInvalido
            // La función manda en "details" cuál condición falló
            e.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                when ((e.details as? Map<*, *>)?.get("motivo")) {
                    "ya_evaluada" -> EvaluacionError.YaEvaluada
                    "no_asignada" -> EvaluacionError.NoAsignada
                    "faltan_documentos" -> EvaluacionError.FaltanDocumentos // HU09
                    else -> EvaluacionError.Servidor(e.message ?: "Condición no cumplida")
                }
            else -> EvaluacionError.Servidor(e.message ?: "Error en el servidor")
        }
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> EvaluacionError.SinConexion
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> EvaluacionError.SinPermiso
            else -> EvaluacionError.Desconocido
        }

        // HU09: la subida del PDF de rechazo
        is StorageException -> when (e.errorCode) {
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> EvaluacionError.SinConexion
            StorageException.ERROR_NOT_AUTHORIZED,
            StorageException.ERROR_NOT_AUTHENTICATED -> EvaluacionError.SinPermiso
            else -> EvaluacionError.FaltanDocumentos
        }

        is FirebaseNetworkException, is IOException -> EvaluacionError.SinConexion
        else -> EvaluacionError.Desconocido
    }

    private companion object {
        const val TAG = "EvaluacionObra"
        const val OBRAS = "obras"
        const val EXPERTOS = "expertos"
    }
}