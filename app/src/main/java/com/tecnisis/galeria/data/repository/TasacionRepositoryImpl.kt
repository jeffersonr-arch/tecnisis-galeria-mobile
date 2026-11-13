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
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.MontosTasacion
import com.tecnisis.galeria.domain.model.ObraPorTasar
import com.tecnisis.galeria.domain.model.ObraTasada
import com.tecnisis.galeria.domain.model.TasacionError
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.documentoVentaDe
import com.tecnisis.galeria.domain.model.rutaDocumentoVenta
import com.tecnisis.galeria.domain.repository.TasacionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException

/**
 * HU10: Firestore en tiempo real para el Especialista de Ventas, el documento de venta y la Cloud Function
 * registrarTasacion. Las reglas solo le dejan leer las obras "aceptada", por eso la consulta lleva ese filtro
 * (un solo filtro: no necesita índice compuesto). El orden lo ponen los casos de uso.
 * Como en la HU09, el celular genera y sube los 2 PDF ANTES de llamar a la función, que revisa que existan.
 */
class TasacionRepositoryImpl(
    private val contexto: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
    private val generador: GeneradorPdf
) : TasacionRepository {

    private val metadatosPdf = StorageMetadata.Builder().setContentType("application/pdf").build()

    override fun observarPorTasar(): Flow<List<ObraPorTasar>> = callbackFlow {
        val registro = db.collection(OBRAS).whereEqualTo("estado", EstadoObra.ACEPTADA.valorFirestore)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapear(error))
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map { it.aObraPorTasar() })
            }
        awaitClose { registro.remove() }
    }

    override fun observarTasadas(): Flow<List<ObraTasada>> = callbackFlow {
        val registro = db.collection(DOCUMENTOS_VENTA).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapear(error))
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().map { it.aObraTasada() })
        }
        awaitClose { registro.remove() }
    }

    override suspend fun obtenerPorTasar(id: String): Result<ObraPorTasar> =
        try {
            Result.success(leerObraPorTasar(id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo leer la obra $id", e)
            Result.failure(mapear(e, leyendoObra = true))
        }

    override suspend fun registrarTasacion(obraId: String, montos: MontosTasacion): Result<Unit> =
        try {
            subirDocumentoVenta(obraId, montos)
            functions.getHttpsCallable("registrarTasacion")
                .call(
                    mapOf(
                        "obraId" to obraId,
                        "pvpCentimos" to montos.pvpCentimos.toInt(), // máximo 100 000 000: entra en un Int
                        "porcentaje" to montos.porcentaje
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo registrar la tasación", e)
            Result.failure(mapear(e, leyendoObra = true))
        }

    /** La obra leída del servidor (sin internet falla aquí). Si ya no está "aceptada", ya se tasó. */
    private suspend fun leerObraPorTasar(id: String): ObraPorTasar {
        val doc = db.collection(OBRAS).document(id).get(Source.SERVER).await()
        if (!doc.exists()) throw TasacionError.NoEncontrada
        val obra = doc.aObraPorTasar()
        if (!obra.porTasar) throw TasacionError.YaTasada
        return obra
    }

    /** Dibuja las 2 copias en cache/documentos y las sube a documentos_venta/{obraId}/. */
    private suspend fun subirDocumentoVenta(obraId: String, montos: MontosTasacion) {
        val uid = auth.currentUser?.uid ?: throw TasacionError.SinPermiso
        // Lo que va en el PDF, leído del servidor mientras la obra sigue "aceptada"
        val obra = leerObraPorTasar(obraId)
        // Ventas no tiene ficha en expertos: su nombre sale de usuarios/{uid} (puede venir vacío)
        val nombre = db.collection(USUARIOS).document(uid).get(Source.SERVER).await().getString("nombre").orEmpty()
        val documento = documentoVentaDe(obra, montos, nombre, System.currentTimeMillis())

        CopiaDocumento.entries.forEach { copia ->
            val archivo = File(File(contexto.cacheDir, "documentos"), "venta-$obraId-${copia.archivo}")
            try {
                generador.generarVenta(documento, copia, archivo)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo generar ${copia.archivo}", e)
                throw TasacionError.PdfNoGenerado
            }
            storage.reference.child(rutaDocumentoVenta(obraId, copia))
                .putFile(Uri.fromFile(archivo), metadatosPdf).await()
        }
    }

    private fun DocumentSnapshot.aObraPorTasar() = ObraPorTasar(
        id = id,
        solicitudId = getString("solicitudId").orEmpty(),
        uid = getString("uid").orEmpty(),
        titulo = getString("titulo").orEmpty(),
        tecnica = Tecnica.desdeValor(getString("tecnica")) ?: Tecnica.TECNICA_MIXTA,
        altoCm = getDouble("altoCm") ?: 0.0,
        anchoCm = getDouble("anchoCm") ?: 0.0,
        profundidadCm = getDouble("profundidadCm"),
        fotoRuta = getString("fotoRuta").orEmpty(),
        nombreSocio = getString("nombreSocio"),
        numeroSocio = getString("numeroSocio"),
        comentarioVeredicto = getString("veredictoComentario")?.takeIf { it.isNotBlank() },
        fechaVeredicto = getTimestamp("fechaVeredicto")?.toDate()?.time,
        estado = EstadoObra.desdeValor(getString("estado"))
    )

    private fun DocumentSnapshot.aObraTasada() = ObraTasada(
        obraId = id,
        titulo = getString("titulo").orEmpty(),
        tecnica = Tecnica.desdeValor(getString("tecnica")) ?: Tecnica.TECNICA_MIXTA,
        fotoRuta = getString("fotoRuta").orEmpty(),
        nombreSocio = getString("nombreSocio"),
        numeroSocio = getString("numeroSocio"),
        pvpCentimos = getLong("pvpCentimos") ?: 0L,
        porcentaje = getLong("porcentaje")?.toInt() ?: 0,
        montoGaleriaCentimos = getLong("montoGaleriaCentimos") ?: 0L,
        montoSocioCentimos = getLong("montoSocioCentimos") ?: 0L,
        fechaTasacion = getTimestamp("fechaTasacion")?.toDate()?.time
    )

    /**
     * leyendoObra: al leer una sola obra, PERMISSION_DENIED significa que ya no está "aceptada"
     * (la regla solo deja leer las aceptadas), es decir, que alguien ya la tasó.
     */
    private fun mapear(e: Exception, leyendoObra: Boolean = false): TasacionError = when (e) {
        is TasacionError -> e
        is FirebaseFunctionsException -> when {
            e.cause is IOException -> TasacionError.SinConexion
            e.code == FirebaseFunctionsException.Code.UNAVAILABLE ||
                    e.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> TasacionError.SinConexion
            e.code == FirebaseFunctionsException.Code.PERMISSION_DENIED ||
                    e.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> TasacionError.SinPermiso
            e.code == FirebaseFunctionsException.Code.NOT_FOUND -> TasacionError.NoEncontrada
            e.code == FirebaseFunctionsException.Code.INVALID_ARGUMENT -> TasacionError.PvpInvalido
            // La función manda en "details" cuál condición falló
            e.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                when ((e.details as? Map<*, *>)?.get("motivo")) {
                    "ya_tasada" -> TasacionError.YaTasada
                    "faltan_documentos" -> TasacionError.FaltanDocumentos
                    else -> TasacionError.Servidor(e.message ?: "Condición no cumplida")
                }
            else -> TasacionError.Servidor(e.message ?: "Error en el servidor")
        }
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> TasacionError.SinConexion
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                if (leyendoObra) TasacionError.YaTasada else TasacionError.SinPermiso
            else -> TasacionError.Desconocido
        }
        // La subida del PDF de venta
        is StorageException -> when (e.errorCode) {
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> TasacionError.SinConexion
            StorageException.ERROR_NOT_AUTHORIZED,
            StorageException.ERROR_NOT_AUTHENTICATED -> TasacionError.SinPermiso
            else -> TasacionError.FaltanDocumentos
        }
        is FirebaseNetworkException, is IOException -> TasacionError.SinConexion
        else -> TasacionError.Desconocido
    }

    private companion object {
        const val TAG = "Tasacion"
        const val OBRAS = "obras"
        const val USUARIOS = "usuarios"
        const val DOCUMENTOS_VENTA = "documentos_venta"
    }
}