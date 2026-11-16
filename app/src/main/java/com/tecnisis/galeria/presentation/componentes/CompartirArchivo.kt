package com.tecnisis.galeria.presentation.componentes

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Abre el menú Compartir de Android (WhatsApp, Drive, correo…) con un archivo de la caché (cache/documentos).
 * FileProvider le presta el archivo a la app elegida con un content:// de solo lectura (igual que en abrirPdf).
 * Devuelve false si no se pudo abrir el menú.
 */
fun compartirArchivo(contexto: Context, ruta: String, tipoMime: String, titulo: String): Boolean {
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", File(ruta))
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = tipoMime
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, titulo)
        clipData = ClipData.newRawUri(titulo, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        contexto.startActivity(Intent.createChooser(envio, "Compartir reporte"))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}