package com.tecnisis.galeria.presentation.componentes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * HU07: abre un PDF que la app descargó en su caché (cache/documentos) con la app de PDF del celular.
 * FileProvider le presta el archivo a esa app con un content:// de solo lectura (igual que con la cámara).
 * Devuelve false si el celular no tiene ninguna app que abra PDF.
 */
fun abrirPdf(contexto: Context, ruta: String): Boolean {
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", File(ruta))
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        contexto.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}