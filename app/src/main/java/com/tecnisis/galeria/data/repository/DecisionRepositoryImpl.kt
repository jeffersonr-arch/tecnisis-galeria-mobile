package com.tecnisis.galeria.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.tecnisis.galeria.data.pdf.GeneradorPdf
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DecisionError
import com.tecnisis.galeria.domain.model.DecisionSolicitud
import com.tecnisis.galeria.domain.model.DocumentoSolicitud
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.ObraEnRevision
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.TipoDecision
import com.tecnisis.galeria.domain.model.filasDocumento
import com.tecnisis.galeria.domain.model.numeroDocumento
import com.tecnisis.galeria.domain.model.rutaDocumento
import com.tecnisis.galeria.domain.repository.DecisionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException

/**
 * HU07 · Parte 2: celular del Anfitrión → 2 PDF (PdfDocument) → Storage → Cloud Function decidirSolicitud.
 * La función revisa que los 2 PDF existan antes de cambiar nada, por eso se suben primero.
 */
class DecisionRepositoryImpl(
    private val contexto: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
    private val generador: GeneradorPdf
) : DecisionRepository {

    private val metadatosPdf = StorageMetadata.Builder().setContentType("application/pdf").build()

    /** Cache/documentos: la misma carpeta que FileProvider comparte con la app de PDF (rutas_fotos.xml). */
    private val carpeta: File
        get() = File(contexto.cacheDir, "documentos")

    override suspend fun decidir(decision: DecisionSolicitud): Result<Unit> = ejecutar {
        val anfitrion = auth.currentUser ?: throw DecisionError.SinPermiso

        // 1) Lo que va en el PDF, leído del servidor (no de la caché del celular)
        val solicitud = db.collection(SOLICITUDES).document(decision.solicitudId).get(Source.SERVER).await()
        if (!solicitud.exists()) throw DecisionError.NoEncontrada
        if (solicitud.getString("estado") != EstadoSolicitud.ENVIADA.valorFirestore) throw DecisionError.YaResuelta
        val socio = db.collection(SOCIOS).document(solicitud.getString("uid").orEmpty()).get(Source.SERVER).await()
        val obras = db.collection(OBRAS).whereEqualTo("solicitudId", decision.solicitudId)
            .get(Source.SERVER).await().documents
            .sortedBy { it.getTimestamp("fechaRegistro")?.toDate()?.time ?: 0L }
            .map { it.aObra() }
        val nombres = decision.asignaciones.values.distinct().associateWith { uid ->
            db.collection(EXPERTOS).document(uid).get(Source.SERVER).await().getString("nombre") ?: "Experto"
        }

        val documento = DocumentoSolicitud(
            tipo = decision.tipo,
            numero = numeroDocumento(decision.solicitudId),
            fecha = System.currentTimeMillis(),
            nombreSocio = socio.getString("nombreCompleto").orEmpty(),
            numeroSocio = socio.getString("numeroSocio").orEmpty(),
            dni = socio.getString("dni").orEmpty(),
            correo = socio.getString("correo").orEmpty(),
            filas = filasDocumento(obras, decision.tipo, decision.asignaciones) { uid -> nombres[uid] ?: "Experto" },
            motivo = decision.motivo.takeIf { decision.tipo == TipoDecision.RECHAZAR },
            resueltoPor = anfitrion.email.orEmpty()
        )

        // 2) Las 2 copias: se dibujan en la caché del celular y se suben a documentos/{solicitudId}/
        CopiaDocumento.entries.forEach { copia ->
            val archivo = File(carpeta, "${decision.solicitudId}-${copia.archivo}")
            try {
                generador.generar(documento, copia, archivo)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo generar ${copia.archivo}", e)
                throw DecisionError.PdfNoGenerado
            }
            storage.reference.child(rutaDocumento(decision.solicitudId, copia))
                .putFile(Uri.fromFile(archivo), metadatosPdf).await()
        }

        // 3) La Cloud Function hace el cambio en una transacción y avisa al socio
        functions.getHttpsCallable("decidirSolicitud")
            .call(
                mapOf(
                    "solicitudId" to decision.solicitudId,
                    "decision" to decision.tipo.valorFuncion,
                    "asignaciones" to decision.asignaciones,
                    "motivo" to decision.motivo
                )
            )
            .await()
        Unit
    }

    override suspend fun descargarDocumento(solicitudId: String, copia: CopiaDocumento): Result<String> = ejecutar {
        val archivo = File(carpeta, "$solicitudId-${copia.archivo}")
        archivo.parentFile?.mkdirs()
        storage.reference.child(rutaDocumento(solicitudId, copia)).getFile(archivo).await()
        archivo.absolutePath
    }

    /** Solo lo que necesita el PDF: título, técnica y medidas. */
    private fun DocumentSnapshot.aObra() = ObraEnRevision(
        id = id,
        titulo = getString("titulo").orEmpty(),
        tecnica = Tecnica.desdeValor(getString("tecnica")) ?: Tecnica.TECNICA_MIXTA,
        altoCm = getDouble("altoCm") ?: 0.0,
        anchoCm = getDouble("anchoCm") ?: 0.0,
        profundidadCm = getDouble("profundidadCm"),
        nota = "",
        fotoRuta = "",
        descripcionIa = null,
        candidatos = emptyList(),
        recomendadoUid = null
    )

    private suspend fun <T> ejecutar(bloque: suspend () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e !is DecisionError) Log.e(TAG, "Error al decidir o descargar", e)
            Result.failure(mapear(e))
        }

    private fun mapear(e: Exception): DecisionError = when (e) {
        is DecisionError -> e
        is FirebaseFunctionsException -> when {
            e.cause is IOException -> DecisionError.SinConexion
            e.code == FirebaseFunctionsException.Code.UNAVAILABLE ||
                    e.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> DecisionError.SinConexion
            e.code == FirebaseFunctionsException.Code.PERMISSION_DENIED ||
                    e.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> DecisionError.SinPermiso
            e.code == FirebaseFunctionsException.Code.NOT_FOUND -> DecisionError.NoEncontrada
            // La función manda en "details" cuál condición falló
            e.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                when ((e.details as? Map<*, *>)?.get("motivo")) {
                    "ya_resuelta" -> DecisionError.YaResuelta
                    "experto_no_disponible" -> DecisionError.ExpertoNoDisponible
                    else -> DecisionError.Servidor(e.message ?: "Condición no cumplida")
                }
            else -> DecisionError.Servidor(e.message ?: "Error en el servidor")
        }
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> DecisionError.SinConexion
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> DecisionError.SinPermiso
            else -> DecisionError.Desconocido
        }
        is StorageException -> when (e.errorCode) {
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> DecisionError.SinConexion
            StorageException.ERROR_NOT_AUTHORIZED,
            StorageException.ERROR_NOT_AUTHENTICATED -> DecisionError.SinPermiso
            StorageException.ERROR_OBJECT_NOT_FOUND -> DecisionError.NoEncontrada
            else -> DecisionError.Desconocido
        }
        is FirebaseNetworkException, is IOException -> DecisionError.SinConexion
        else -> DecisionError.Desconocido
    }

    private companion object {
        const val TAG = "DecisionSolicitud"
        const val SOLICITUDES = "solicitudes"
        const val OBRAS = "obras"
        const val SOCIOS = "socios"
        const val EXPERTOS = "expertos"
    }
}