package com.tecnisis.galeria.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.tecnisis.galeria.domain.model.ObraError
import com.tecnisis.galeria.domain.repository.FotoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

/** HU06: fotos de las obras en la memoria interna de la app (files/obras), reducidas para que suban rápido. */
class FotoRepositoryImpl(private val contexto: Context) : FotoRepository {

    private val carpetaCamara: File
        get() = File(contexto.cacheDir, "camara").apply { mkdirs() }
    private val carpetaObras: File
        get() = File(contexto.filesDir, "obras").apply { mkdirs() }

    override fun crearDestinoCamara(): String {
        val archivo = File(carpetaCamara, "captura_${System.currentTimeMillis()}.jpg")
        // FileProvider convierte la ruta privada en un content:// que la app de cámara puede escribir
        return FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", archivo).toString()
    }

    override suspend fun prepararFoto(origen: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(origen)
            val reducida = leerReducida(uri) ?: throw ObraError.FotoNoLeida
            val derecha = girar(reducida, gradosExif(uri))
            if (derecha !== reducida) reducida.recycle()

            val destino = File(carpetaObras, "${UUID.randomUUID()}.jpg")
            FileOutputStream(destino).use { salida ->
                derecha.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPG, salida)
            }
            derecha.recycle()
            carpetaCamara.listFiles()?.forEach { it.delete() } // la captura original ya no se necesita
            Result.success(destino.absolutePath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ObraError.FotoNoLeida)
        }
    }

    override fun borrarFoto(ruta: String) {
        val archivo = File(ruta)
        // Solo borra dentro de files/obras: nunca otra cosa del celular
        if (archivo.parentFile?.canonicalPath == carpetaObras.canonicalPath) archivo.delete()
    }

    /** Lee la foto ya reducida: primero mide sin cargarla y luego carga solo lo necesario. */
    private fun leerReducida(uri: Uri): Bitmap? {
        val resolver = contexto.contentResolver
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val entrada = resolver.openInputStream(uri) ?: return null
        entrada.use { BitmapFactory.decodeStream(it, null, medidas) }
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

        var muestra = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: return null

        val lado = maxOf(bitmap.width, bitmap.height)
        if (lado <= LADO_MAXIMO) return bitmap
        val escala = LADO_MAXIMO.toFloat() / lado
        val reducida = Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * escala).roundToInt(), (bitmap.height * escala).roundToInt(), true
        )
        if (reducida !== bitmap) bitmap.recycle()
        return reducida
    }

    /** Muchos celulares guardan la foto "echada" y anotan en el EXIF cuánto hay que girarla. */
    private fun gradosExif(uri: Uri): Int = try {
        contexto.contentResolver.openInputStream(uri)?.use { entrada ->
            when (ExifInterface(entrada).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (e: Exception) {
        0
    }

    private fun girar(bitmap: Bitmap, grados: Int): Bitmap {
        if (grados == 0) return bitmap
        val matriz = Matrix().apply { postRotate(grados.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }

    private companion object {
        const val LADO_MAXIMO = 1600
        const val CALIDAD_JPG = 80
    }
}