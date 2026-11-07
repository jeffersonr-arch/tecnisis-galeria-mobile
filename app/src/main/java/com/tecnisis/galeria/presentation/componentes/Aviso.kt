package com.tecnisis.galeria.presentation.componentes

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/**
 * HU07: franja de aviso (analizando, error, falta Experto). Lo de abajo (contenido) es opcional,
 * por ejemplo un botón "Reintentar".
 */
@Composable
fun Aviso(
    titulo: String,
    texto: String,
    icono: ImageVector,
    fondo: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = fondo) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icono, contentDescription = null, tint = colorTexto, modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(titulo, style = MaterialTheme.typography.titleSmall, color = colorTexto)
                    Text(texto, style = MaterialTheme.typography.bodySmall, color = colorTexto)
                }
            }
            contenido()
        }
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AvisoPreview() {
    TecnisisGaleriaTheme {
        val c = MaterialTheme.colorScheme
        Surface(color = c.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Aviso(
                    "Analizando las obras con IA",
                    "Se describe cada obra y se busca el Experto más afín. Puede tardar hasta un minuto.",
                    Icons.Outlined.AutoAwesome, c.primaryContainer, c.onPrimaryContainer
                )
                Aviso(
                    "No se pudo analizar la solicitud",
                    "Revisa tu conexión a internet e inténtalo de nuevo.",
                    Icons.Outlined.CloudOff, c.errorContainer, c.onErrorContainer
                ) {
                    OutlinedButton(onClick = {}) { Text("Reintentar") }
                }
                Aviso(
                    "Una obra no tiene Experto disponible",
                    "No puedes aprobar: rechaza la solicitud e indica el motivo al socio.",
                    Icons.Outlined.ErrorOutline, c.errorContainer, c.onErrorContainer
                )
            }
        }
    }
}