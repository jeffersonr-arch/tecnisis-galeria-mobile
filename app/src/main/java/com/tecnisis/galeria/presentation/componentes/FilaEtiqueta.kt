package com.tecnisis.galeria.presentation.componentes

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/**
 * HU08: una etiqueta que ML Kit detectó en la foto, con su porcentaje de confianza (0 a 100).
 * traducida = false: no está en el diccionario y se muestra en inglés, con la nota "sin traducción".
 */
@Composable
fun FilaEtiqueta(nombre: String, porcentaje: Int, traducida: Boolean, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.onSurface)) { append(nombre) }
                    if (!traducida) {
                        withStyle(SpanStyle(color = c.onSurfaceVariant)) { append(" · sin traducción") }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text("$porcentaje %", style = MaterialTheme.typography.labelLarge, color = c.primary)
        }
        LinearProgressIndicator(
            progress = { (porcentaje / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = c.primary,
            trackColor = c.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilaEtiquetaPreview() {
    TecnisisGaleriaTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilaEtiqueta("Cielo", 91, traducida = true)
                FilaEtiqueta("Barco", 78, traducida = true)
                FilaEtiqueta("Brig", 52, traducida = false)
            }
        }
    }
}