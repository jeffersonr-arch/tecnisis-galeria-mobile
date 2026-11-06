package com.tecnisis.galeria.presentation.componentes

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@Composable
fun ChipColor(texto: String, fondo: Color, colorTexto: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colorTexto)
        )
        Text(texto, style = MaterialTheme.typography.labelMedium, color = colorTexto, maxLines = 1)
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChipColorPreview() {
    TecnisisGaleriaTheme {
        val c = MaterialTheme.colorScheme
        Surface(color = c.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipColor("Por analizar", c.surfaceVariant, c.onSurfaceVariant)
                ChipColor("Lista para decidir", c.primaryContainer, c.onPrimaryContainer)
                ChipColor("Falta Experto", c.errorContainer, c.onErrorContainer)
                ChipColor("Aprobada", c.tertiaryContainer, c.onTertiaryContainer)
            }
        }
    }
}