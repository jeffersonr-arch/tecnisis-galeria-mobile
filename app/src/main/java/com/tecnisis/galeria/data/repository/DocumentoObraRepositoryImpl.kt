package com.tecnisis.galeria.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DecisionError
import com.tecnisis.galeria.domain.model.rutaDocumentoObra
import com.tecnisis.galeria.domain.repository.DocumentoObraRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException

/**
 * HU09: baja una copia de documentos_obras/{obraId}/ a cache/documentos (la carpeta que FileProvider comparte
 * con la app de PDF). Quién puede leer cada copia lo deciden las reglas de Storage.
 */
class DocumentoObraRepositoryImpl(
    private val contexto: Context,
    private val storage: FirebaseStorage
) : DocumentoObraRepository {

    override suspend fun descargar(obraId: String, copia: CopiaDocumento): Result<String> =
        try {
            val archivo = File(File(contexto.cacheDir, "documentos"), "obra-$obraId-${copia.archivo}")
            archivo.parentFile?.mkdirs()
            storage.reference.child(rutaDocumentoObra(obraId, copia)).getFile(archivo).await()
            Result.success(archivo.absolutePath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo descargar el documento de la obra $obraId", e)
            Result.failure(mapear(e))
        }

    private fun mapear(e: Exception): DecisionError = when (e) {
        is StorageException -> when (e.errorCode) {
            StorageException.ERROR_OBJECT_NOT_FOUND -> DecisionError.NoEncontrada // rechazada antes de la HU09
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> DecisionError.SinConexion
            StorageException.ERROR_NOT_AUTHORIZED,
            StorageException.ERROR_NOT_AUTHENTICATED -> DecisionError.SinPermiso
            else -> DecisionError.Desconocido
        }
        is FirebaseNetworkException, is IOException -> DecisionError.SinConexion
        else -> DecisionError.Desconocido
    }

    private companion object {
        const val TAG = "DocumentoObra"
    }
}