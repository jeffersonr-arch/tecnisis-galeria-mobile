package com.tecnisis.galeria.data.repository

import android.content.Context
import android.util.Log
import com.tecnisis.galeria.data.pdf.GeneradorPdf
import com.tecnisis.galeria.domain.model.ArchivoReporte
import com.tecnisis.galeria.domain.model.FormatoReporte
import com.tecnisis.galeria.domain.model.ProyeccionEconomica
import com.tecnisis.galeria.domain.model.ProyeccionError
import com.tecnisis.galeria.domain.model.nombreReporte
import com.tecnisis.galeria.domain.model.textoCsv
import com.tecnisis.galeria.domain.repository.ReporteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.TimeZone

/**
 * HU12: escribe el reporte en cache/documentos (la carpeta que FileProvider comparte), con la fecha y la hora
 * en el nombre para que una exportación nueva no pise a la anterior mientras se comparte.
 */
class ReporteRepositoryImpl(
    private val contexto: Context,
    private val generadorPdf: GeneradorPdf
) : ReporteRepository {

    override suspend fun generar(
        proyeccion: ProyeccionEconomica,
        formato: FormatoReporte,
        generado: Long
    ): Result<ArchivoReporte> = withContext(Dispatchers.IO) {
        try {
            val zona = TimeZone.getDefault()
            val archivo = File(File(contexto.cacheDir, "documentos"), nombreReporte(generado, zona, formato))
            archivo.parentFile?.mkdirs()
            when (formato) {
                FormatoReporte.PDF -> generadorPdf.generarProyeccion(proyeccion, generado, zona, archivo)
                // UTF-8 con BOM: así Excel muestra bien las tildes
                FormatoReporte.CSV -> archivo.outputStream().use { salida ->
                    salida.write(BOM)
                    salida.write(textoCsv(proyeccion, zona).toByteArray(Charsets.UTF_8))
                }
            }
            Result.success(ArchivoReporte(archivo.absolutePath, formato))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo generar el reporte ${formato.extension}", e)
            Result.failure(ProyeccionError.NoSeGenero)
        }
    }

    private companion object {
        const val TAG = "Reporte"
        val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}