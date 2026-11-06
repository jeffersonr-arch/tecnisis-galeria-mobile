package com.tecnisis.galeria.presentation.componentes

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import com.tecnisis.galeria.presentation.theme.galeria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * HU07: descarga y decodifica la foto de una obra que está en Storage.
 * descargar viene del ViewModel (caso de uso); devuelve null si falla y se queda el dibujo de pigmentos.
 */
@Composable
fun rememberFotoRemota(
    ruta: String?,
    descargar: suspend (String) -> ByteArray?,
    ladoMaximo: Int = 800
): ImageBitmap? {
    val imagen by produceState<ImageBitmap?>(initialValue = null, ruta) {
        value = if (ruta.isNullOrBlank()) null else {
            descargar(ruta)?.let { bytes -> withContext(Dispatchers.Default) { decodificar(bytes, ladoMaximo) } }
        }
    }
    return imagen
}

/** Reduce la imagen al decodificarla (inSampleSize) para no gastar memoria en miniaturas. */
private fun decodificar(bytes: ByteArray, ladoMaximo: Int): ImageBitmap? {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, medidas)
    var muestra = 1
    while (maxOf(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= ladoMaximo) muestra *= 2
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)?.asImageBitmap()
}

@Composable
fun FotoRemota(imagen: ImageBitmap?, modifier: Modifier = Modifier) {
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

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FotoRemotaPreview() {
    TecnisisGaleriaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FotoRemota(imagen = null, modifier = Modifier.size(40.dp))
                FotoRemota(imagen = null, modifier = Modifier.size(120.dp))
            }
        }
    }
}