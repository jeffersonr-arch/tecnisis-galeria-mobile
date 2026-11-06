package com.tecnisis.galeria.presentation.componentes

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import com.tecnisis.galeria.presentation.theme.galeria

/** "Carmen Villanueva" → "CV". */
fun iniciales(nombre: String): String =
    nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/**
 * HU07: el Experto de una obra, con su afinidad (0 a 100) y su carga (de 5).
 * onCambiar null = sin botón "Cambiar" (por ejemplo, cuando la solicitud ya se resolvió).
 */
@Composable
fun FilaExperto(
    rotulo: String,
    nombre: String,
    tecnicas: String,
    afinidad: Int,
    carga: Int,
    cargaMaxima: Int,
    onCambiar: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    val g = MaterialTheme.galeria
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSeccion(rotulo)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(c.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(iniciales(nombre), style = MaterialTheme.typography.labelLarge, color = c.onSecondaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text(nombre, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                Text(tecnicas, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
            if (onCambiar != null) {
                TextButton(onClick = onCambiar) { Text("Cambiar") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Medidor("Afinidad", "$afinidad %", afinidad / 100f, c.primary, Modifier.weight(1f))
            Medidor("Carga", "$carga de $cargaMaxima", carga.toFloat() / cargaMaxima, g.pigmentoAmarillo, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Medidor(etiqueta: String, valor: String, progreso: Float, color: Color, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            buildAnnotatedString {
                append("$etiqueta ")
                withStyle(SpanStyle(color = c.onSurface, fontWeight = FontWeight.Bold)) { append(valor) }
            },
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { progreso.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
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
private fun FilaExpertoPreview() {
    TecnisisGaleriaTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                FilaExperto(
                    rotulo = "Experto recomendado", nombre = "Carmen Villanueva",
                    tecnicas = "Óleo · Acrílico · Técnica mixta", afinidad = 87, carga = 2, cargaMaxima = 5,
                    onCambiar = {}
                )
                FilaExperto(
                    rotulo = "Experto asignado", nombre = "Jorge Huamán Ccori",
                    tecnicas = "Textil · Cerámica", afinidad = 91, carga = 1, cargaMaxima = 5,
                    onCambiar = null
                )
            }
        }
    }
}