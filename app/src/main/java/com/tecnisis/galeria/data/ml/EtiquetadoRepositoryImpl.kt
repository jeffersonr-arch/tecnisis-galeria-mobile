package com.tecnisis.galeria.data.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.tecnisis.galeria.domain.model.CONFIANZA_MINIMA
import com.tecnisis.galeria.domain.model.EtiquetaCruda
import com.tecnisis.galeria.domain.model.EvaluacionError
import com.tecnisis.galeria.domain.repository.EtiquetadoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * HU08: ML Kit Image Labeling con el modelo incluido en la app (com.google.mlkit:image-labeling).
 * Corre en el celular, sin internet y sin costo. Recibe los mismos bytes de la foto que ya se descargaron
 * de Storage para mostrarla (no se descarga dos veces).
 */
class EtiquetadoRepositoryImpl : EtiquetadoRepository {

    // El umbral también va aquí para que ML Kit no devuelva etiquetas que de todos modos no se muestran
    private val opciones = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(CONFIANZA_MINIMA)
        .build()

    override suspend fun etiquetar(foto: ByteArray): Result<List<EtiquetaCruda>> =
        try {
            val imagen = withContext(Dispatchers.Default) { decodificar(foto) }
                ?: throw EvaluacionError.FotoNoAnalizada
            val etiquetador = ImageLabeling.getClient(opciones)
            try {
                val etiquetas = etiquetador.process(InputImage.fromBitmap(imagen, 0)).await()
                Result.success(etiquetas.map { EtiquetaCruda(it.text, it.confidence) })
            } finally {
                etiquetador.close()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit no pudo analizar la foto", e)
            Result.failure(EvaluacionError.FotoNoAnalizada)
        }

    /** El JPG puede pesar hasta 5 MB: se reduce a unos 1024 px de lado, suficiente para clasificar. */
    private fun decodificar(bytes: ByteArray): Bitmap? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, medidas)
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null
        var muestra = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
    }

    private companion object {
        const val TAG = "EtiquetadoMlKit"
        const val LADO_MAXIMO = 1024
    }
}