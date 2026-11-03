package com.tecnisis.galeria.presentation.componentes

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.tecnisis.galeria.presentation.theme.galeria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Carga en segundo plano una foto guardada en el celular, reducida a ladoMaximo px. Es null mientras carga. */
@Composable
fun rememberImagenLocal(ruta: String?, ladoMaximo: Int = 800): ImageBitmap? {
    val imagen by produceState<ImageBitmap?>(initialValue = null, ruta, ladoMaximo) {
        value = if (ruta.isNullOrBlank()) null else withContext(Dispatchers.IO) { leerImagen(ruta, ladoMaximo) }
    }
    return imagen
}

private fun leerImagen(ruta: String, ladoMaximo: Int): ImageBitmap? {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(ruta, medidas)
    if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null
    var muestra = 1
    while (maxOf(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= ladoMaximo) muestra *= 2
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
    return BitmapFactory.decodeFile(ruta, opciones)?.asImageBitmap()
}

/** Foto de la obra recortada al tamaño del modifier. Sin foto (o en la Preview) dibuja manchas de pigmento. */
@Composable
fun FotoObra(ruta: String?, modifier: Modifier = Modifier, ladoMaximo: Int = 800) {
    val imagen = rememberImagenLocal(ruta, ladoMaximo)
    val c = MaterialTheme.colorScheme
    val g = MaterialTheme.galeria
    Box(modifier.clip(MaterialTheme.shapes.small).background(c.surfaceVariant)) {
        if (imagen != null) {
            Image(
                bitmap = imagen,
                contentDescription = "Foto de la obra",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val lado = size.minDimension
                drawCircle(c.primary, radius = lado * 0.24f, center = Offset(size.width * 0.38f, size.height * 0.42f))
                drawCircle(g.pigmentoAmarillo, radius = lado * 0.17f, center = Offset(size.width * 0.64f, size.height * 0.44f))
                drawCircle(g.pigmentoRojo, radius = lado * 0.16f, center = Offset(size.width * 0.52f, size.height * 0.68f))
            }
        }
    }
}